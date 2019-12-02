package es.uvigo.esei.dai.hybridserver.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.io.HybridInputStream;
import es.uvigo.esei.dai.hybridserver.io.ReaderInputStreamAdapter;

/**
 * Models a HTTP request.
 *
 * @author Alejandro González García
 * @implNote This class is thread-safe.
 */
public final class HTTPRequest {
	/**
	 * Compile and store the potentially complex regular expression for speed. This
	 * regex is a straightforward translation of the BNF rules for abs_path in RFC
	 * 2396, adding a query string to the end.
	 */
	private static final Pattern RELATIVE_URI_REGEX = Pattern.compile(
		"^/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*)*(?:/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*)*)*(?:\\?(?:[;/?:@&=+$,a-zA-Z0-9_.!~*'()-]|%[0-9a-fA-F]{2})*)?$"
	);

	private static final String DEFAULT_CONTENT_TYPE = "text/plain";

	private final HybridServer server;
	private final HTTPRequestMethod method;
	private final String resourceChain;
	private final String httpVersion;
	private final String[] resourcePath;
	private final String resourceName;
	private final Map<String, String> resourceParameters;
	private final Map<String, String> headerParameters;
	private final ByteBuffer content;
	private final String textContent;
	private final int contentLength;

	/**
	 * Creates a HTTP request object associated to a server by parsing input from a
	 * stream.
	 *
	 * @param server      The server where this HTTP request arrived.
	 * @param inputStream The stream to get a HTTP request from.
	 * @throws IOException        If an I/O error occurs during parsing.
	 * @throws HTTPParseException If the HTTP request has a non-conforming syntax,
	 *                            or some characteristic of it prevents it from
	 *                            being processed by this server. Users should check
	 *                            the cause of this exception to know whether it is
	 *                            a bad request or the server just lacks support for
	 *                            it.
	 */
	// We do not want to close the HybridInputStream, as it will close the socket
	// too, and that's not the responsibility of this class
	@SuppressWarnings("resource")
	public HTTPRequest(final HybridServer server, final InputStream inputStream) throws IOException, HTTPParseException {
		String inputLine;
		HybridInputStream input;

		if (inputStream == null) {
			throw new IllegalArgumentException("Can't associate a HTTP request with a null Reader");
		}

		input = new HybridInputStream(
			new BufferedInputStream(inputStream),
			StandardCharsets.ISO_8859_1
		);

		// This field may be null
		this.server = server;

		// According to RFC 2616, section 5, a request is defined by
		// the following grammar:
		// Request =	Request-Line
		// 				*(( general-header
		// 				| request-header
		// 				| entity-header ) CRLF)
		// 				CRLF
		// 				[ message-body ]

		// Therefore, start by reading the request line
		inputLine = input.readLine();
		if (inputLine == null) {
			throw new HTTPParseException("Expected the HTTP request line, but reached end of stream");
		}

		// Get and check the correctness of the method, URI and version fields
		final String[] requestLineFields = inputLine.split(" ");
		if (requestLineFields.length != 3) {
			throw new HTTPParseException("Expected three space-separated fields on the HTTP request line, got " + requestLineFields.length);
		}

		try {
			this.method = HTTPRequestMethod.valueOf(requestLineFields[0]);
		} catch (final IllegalArgumentException exc) {
			throw new HTTPParseException("Got an unexpected HTTP request method", exc);
		}

		if (isValidResourceChain(requestLineFields[1])) {
			this.resourceChain = requestLineFields[1];
		} else {
			throw new HTTPParseException("Unsupported or invalid request URI for a HTTP request method");
		}

		if (isSupportedHttpVersion(requestLineFields[2])) {
			this.httpVersion = requestLineFields[2];
		} else {
			throw new HTTPParseException("Unsupported HTTP request version");
		}

		// Deduce the resource path, name and resource parameters. For that,
		// we check where the query starts in the URI, if there is any query
		final Map<String, String> resourceParameters = new LinkedHashMap<>(); // Linked so we preserve ordering
		final int resourceChainQueryStart = resourceChain.indexOf("?");

		if (resourceChainQueryStart > -1) {
			// Do not include the query string as a part of the resource name
			this.resourceName = resourceChain.substring(1, resourceChainQueryStart);

			switch (method) {
				case GET:
				case HEAD:
				case DELETE: {
					// Parse the query string to get the resource parameters, too.
					// Note that the query string format is not completely defined in the standard:
					// the standard just specifies what characters can be in a query string and
					// whether they're reserved or not, not their meaning. So we follow the most
					// common convention and treat it like key=value pairs separated by &
					parseResourceParameters(resourceChain.substring(resourceChainQueryStart + 1), resourceParameters, true);
				}
				default: // Do not parse query string as resource parameters
			}
		} else {
			// No query, so just copy the resource chain without the leading /
			this.resourceName = resourceChain.substring(1);
		}

		// Resource names must escape / when it doesn't serve as a
		// path segment separator, so this split is safe
		if (resourceName.isEmpty()) {
			this.resourcePath = new String[] {};
		} else {
			this.resourcePath = resourceName.split("/");
		}

		// Done with the request line, now take care of the headers
		boolean headersEnd = false;
		int messageBodyLength = -1;
		final Map<String, String> headerParameters = new LinkedHashMap<>(); // Linked so we preserve ordering

		while (!headersEnd && (inputLine = input.readLine()) != null) {
			if (inputLine.isEmpty()) {
				// Stop parsing headers when we reach an empty line
				headersEnd = true;
			} else {
				// Luckily, all standard and non-standard headers I know of obey
				// the format HEADER: VALUE.
				// We do not enforce strict checking here because some applications
				// send non-RFC-standard headers (for example, Firefox can be configured to
				// send a DNT header) and we do not want them to change their behavior
				final String[] headerPair = inputLine.split(": ", 2);
				if (headerPair.length != 2) {
					throw new HTTPParseException("Received a malformed header in HTTP request");
				}

				// No support for transfer encodings
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.TRANSFER_ENCODING.getHeader())) {
					// We associate a HTTPUnsupportedHeaderException as a cause so callers can
					// decide to respond with a 501 status code (Not Implemented) rather than 400
					// (Bad Request), as the standard mandates
					throw new HTTPParseException("This server does not support HTTP request transfer encodings",
						new HTTPUnsupportedHeaderException(HTTPHeaders.TRANSFER_ENCODING)
					);
				}

				// No support for byteranges content type (it affects request length
				// computation)
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.CONTENT_TYPE.getHeader())
					&& headerPair[1].startsWith("multipart/byteranges")
				) {
					// Do not send additional cause information, as this content type must not be
					// used by clients without knowing whether the server supports it, so it would
					// be a bad request
					throw new HTTPParseException("This server does not support the specified HTTP request content type");
				}

				// No support for most content encodings
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.CONTENT_ENCODING.getHeader())) {
					try {
						if (
							!headerPair[1].equalsIgnoreCase("identity") &&
							!Charset.isSupported(headerPair[1]) // Libraries used by tests indicate charsets here
						) {
							throw new HTTPParseException();
						}
					} catch (HTTPParseException | IllegalCharsetNameException exc) {
						throw new HTTPParseException(
							"This server does not support the specified content encoding for HTTP requests",
							new HTTPUnsupportedContentEncodingException(headerPair[1].toLowerCase())
						);
					}
				}

				// Get message body length from the corresponding header
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.CONTENT_LENGTH.getHeader())) {
					try {
						// The parse method is lenient with the standard, because it allows an
						// extra '+' symbol before the digits
						messageBodyLength = Integer.parseInt(headerPair[1]);
						if (messageBodyLength < 0) {
							throw new NumberFormatException();
						}
					} catch (final NumberFormatException exc) {
						throw new HTTPParseException("Invalid Content-Length in HTTP request: expected 32-bit natural number", exc);
					}
				}

				headerParameters.put(headerPair[0], headerPair[1]);
			}
		}

		// If we didn't end the header before we reach end of input,
		// the request headers are malformed
		if (!headersEnd && inputLine == null) {
			throw new HTTPParseException("Expected a white line to end HTTP request headers, but got end of input");
		}

		// Make maps unmodifiable so they can be shared across threads
		// safely, and users can't mess with a map that should be
		// read-only
		this.resourceParameters = Collections.unmodifiableMap(resourceParameters);
		this.headerParameters = Collections.unmodifiableMap(headerParameters);

		// Check whether the server supports the provided character set.
		// This class and its dependencies support any character set, but other
		// parts of the server do not.
		// Although only UTF-8 support is required, as UTF-8 is backwards
		// compatible with US-ASCII and ISO-8859-1, we accept those too
		final Charset bodyCharset = getBodyCharset();
		if (
			bodyCharset != null &&
			!StandardCharsets.UTF_8.equals(bodyCharset) &&
			!StandardCharsets.US_ASCII.equals(bodyCharset) &&
			!StandardCharsets.ISO_8859_1.equals(bodyCharset)
		) {
			throw new HTTPParseException(
				"This server does not support the specified content character set",
				new HTTPUnsupportedContentEncodingException(bodyCharset.displayName())
			);
		}

		// Now comes the message body. After taking into account the simplifications
		// made for this implementation, the standard defines two ways of getting its
		// length (RFC 2616, section 4.4):
		// 1. By the Content-Length header we've read before.
		// 2. By reading until we, the server, close the connection (but "closing the
		// connection cannot be used to indicate the end of a request body").
		// Therefore, ruling out 2., we're left with using the Content-Length header.
		// If we detect that there're more characters ahead and we don't have any
		// Content-Length, we should respond with a 411 status code, because we can't
		// compute the message length reliably. Note that the Content-Length header is
		// only mandatory for requests with a body
		if (messageBodyLength < 0 && input.available() > 0 && input.read() > -1) {
			throw new HTTPParseException("Missing Content-Length header for HTTP request",
				new HTTPMissingHeaderException(HTTPHeaders.CONTENT_LENGTH)
			);
		}

		ByteBuffer bodyBuffer = null;
		if (messageBodyLength > 0) {
			final ReadableByteChannel inputChannel = Channels.newChannel(input);
			bodyBuffer = ByteBuffer.allocate(1024); // 1 KiB initial buffer size
			int totalBytesRead = 0;
			boolean eof = false;

			do {
				final int bytesRead = inputChannel.read(bodyBuffer);
				eof = bytesRead == -1;

				if (bytesRead > 0) {
					totalBytesRead += bytesRead;
				}

				if (!eof && totalBytesRead < messageBodyLength && !bodyBuffer.hasRemaining()) {
					// We ran out of space for this buffer, so allocate a bigger one
					final ByteBuffer newBodyBuffer = ByteBuffer.allocate(bodyBuffer.capacity() * 2);
					bodyBuffer.flip();
					newBodyBuffer.put(bodyBuffer);
					bodyBuffer = newBodyBuffer;
				}
			} while (!eof && totalBytesRead < messageBodyLength);

			bodyBuffer.flip();
		}

		String textContent = null;
		if (bodyBuffer != null && bodyCharset != null) {
			final String contentType = getContentType();

			switch (method) {
				case POST:
				case PUT: {
					// Parse body as resource parameters. The tests require us to follow
					// the same format as for query strings in the URI. Also, use the
					// return value to save iterating over the content again
					final boolean urlEncoded = contentType.startsWith(MIME.FORM.getMime());

					assert bodyBuffer.hasArray() : "A byte buffer with a backing array is needed";
					textContent = parseResourceParameters(
						new String(bodyBuffer.array(), 0, bodyBuffer.limit(), bodyCharset),
						resourceParameters,
						urlEncoded
					);
				}
				default: // Do not parse body as resource parameters
			}

			if (textContent == null) {
				// Decode textual bodies depending on type
				if (contentType.startsWith(MIME.FORM.getMime())) {
					assert bodyBuffer.hasArray() : "A byte buffer with a backing array is needed";
					textContent = URLDecoder.decode(
						new String(bodyBuffer.array(), 0, bodyBuffer.limit(), bodyCharset),
						StandardCharsets.UTF_8.name()
					);
				}
			}
		}

		this.content = bodyBuffer == null ? null : bodyBuffer.asReadOnlyBuffer();
		this.textContent = textContent;
		this.contentLength = Math.max(messageBodyLength, 0);
	}

	/**
	 * Creates a HTTP request object by parsing input from a stream.
	 *
	 * @param input The stream to get a HTTP request from.
	 * @throws IOException        If an I/O error occurs during parsing.
	 * @throws HTTPParseException If the HTTP request has a non-conforming syntax,
	 *                            or some characteristic of it prevents it from
	 *                            being processed by this server. Users should check
	 *                            the cause of this exception to know whether it is
	 *                            a bad request or the server just lacks support for
	 *                            it.
	 */
	public HTTPRequest(final InputStream input) throws IOException, HTTPParseException {
		this(null, input);
	}

	/**
	 * Creates a HTTP request object by parsing input from a reader.
	 *
	 * @param reader The reader to get a HTTP request from.
	 * @throws IOException        If an I/O error occurs during parsing.
	 * @throws HTTPParseException If the HTTP request has a non-conforming syntax,
	 *                            or some characteristic of it prevents it from
	 *                            being processed by this server. Users should check
	 *                            the cause of this exception to know whether it is
	 *                            a bad request or the server just lacks support for
	 *                            it.
	 *
	 * @deprecated Reading an arbitrary HTTP request with a {@link Reader} uses
	 *             complex, additional wrapping layers that affect performance, as a
	 *             {@link Reader} doesn't expose a way to read bytes from the
	 *             underlying input stream. The {@code Content-Length} header, used
	 *             to determine when the HTTP request ends, is expressed in octets
	 *             according to
	 *             <a href="https://tools.ietf.org/html/rfc2616#page-119">RFC
	 *             2616</a>, so reading the request body requires, in order to
	 *             support multibyte encodings (like UTF-8) and binary content,
	 *             access to a stream of bytes. This constructor is only provided
	 *             for compatibility with tests and should be avoided when possible,
	 *             as the wrapping process may incur in information loss.
	 */
	@Deprecated
	public HTTPRequest(final Reader reader) throws IOException, HTTPParseException {
		// Terminal elements of the HTTP request grammar are defined in terms
		// of US-ASCII characters, with some kind of compatibility with ISO-8859-1:
		// "The TEXT rule is only used for descriptive field contents and values
		// that are not intended to be interpreted by the message parser. Words
		// of *TEXT MAY contain characters from character sets other than ISO-
		// 8859-1 [22] only when encoded according to the rules of RFC 2047
		// [14]." - https://tools.ietf.org/html/rfc2616#section-2.2
		// Moreover, this encoding also preserves the equivalence of string length and
		// number of bytes, which is necessary for passing tests. And luckily,
		// the tests don't use characters which don't have an equivalent in
		// ISO-8859-1
		this(null, new ReaderInputStreamAdapter(reader, StandardCharsets.ISO_8859_1));
	}

	/**
	 * Gets the server where this HTTP request has just arrived.
	 *
	 * @return The described Hybrid Server. This value may be null if the server
	 *         where this HTTP request arrived is unknown.
	 */
	public HybridServer getServer() {
		return server;
	}

	/**
	 * Gets the request method of this HTTP request.
	 *
	 * @return The HTTP request method.
	 */
	public HTTPRequestMethod getMethod() {
		return method;
	}

	/**
	 * Gets the resource chain described in this HTTP request, verbatim (as sent by
	 * the client).
	 *
	 * @return The resource chain of this HTTP request.
	 */
	public String getResourceChain() {
		return resourceChain;
	}

	/**
	 * Gets the resource path (path components, e.g. folders and file name) this
	 * HTTPRequest refers to.
	 *
	 * @return The resource path of the request. Users of this method <b>must
	 *         not</b> modify the values contained in the array.
	 */
	public String[] getResourcePath() {
		return resourcePath;
	}

	/**
	 * Gets the resource name (e.g. file name) this HTTPRequest refers to.
	 *
	 * @return The resource name of the request.
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Gets the resource parameters (query string name-value pairs) contained in
	 * this HTTPRequest.
	 *
	 * @return The resource parameters of the request. Users of this method <b>must
	 *         not</b> assume that the returned map is modifiable, and should treat
	 *         it as read-only.
	 */
	public Map<String, String> getResourceParameters() {
		return resourceParameters;
	}

	/**
	 * Obtains the HTTP version of this request.
	 *
	 * @return The HTTP version of this request.
	 */
	public String getHttpVersion() {
		return httpVersion;
	}

	/**
	 * Gets the header parameters contained in this HTTPRequest. They are sent
	 * before the message body, and after the first request line.
	 *
	 * @return The header parameters of the request. Users of this method <b>must
	 *         not</b> assume that the returned map is modifiable, and should treat
	 *         it as read-only.
	 */
	public Map<String, String> getHeaderParameters() {
		return headerParameters;
	}

	/**
	 * Obtains the full body text of this request, sent by the client. This method
	 * interprets the received data as text. If its Content-Type was recognized
	 * while parsing it, it's automatically decoded to its intended form.
	 *
	 * @return The body of this request. If the client didn't send a request body,
	 *         or it is not text (i.e. the {@link HTTPRequest#bodyIsText} method
	 *         returns false), then a null value will be returned.
	 * @see HTTPRequest#contentIsText
	 */
	public String getContent() {
		return textContent;
	}

	/**
	 * Obtains the full body of this request, exactly as sent by the client, in a
	 * binary form, which the caller is free to manipulate as it pleases.
	 *
	 * @return A read-only byte buffer with the bytes that compose the body of this
	 *         request. Its position will be 0 and its limit will be equal to the
	 *         length of the message body. If the client didn't send a request body,
	 *         a null value will be returned.
	 */
	public ByteBuffer getBinaryContent() {
		return content == null ? null : content.duplicate();
	}

	/**
	 * Returns the length of the body of this request. It always is zero or greater.
	 * If multibyte characters are used, the request body interpreted as text might
	 * be shorter than the length returned by this method. Also, even assuming that
	 * one byte corresponds exactly to one character, the content length may be
	 * different to the actual content size because of the decoding done on the
	 * server side. Therefore, the result of this method should not be used when a
	 * precise measurement of the actual storage space taken by the processed body
	 * is needed.
	 *
	 * @return The length of the body of this request, which is the number of body
	 *         bytes the client sent over the network.
	 */
	public int getContentLength() {
		return contentLength;
	}

	/**
	 * Checks whether the message body is to be interpreted as text, according to
	 * the Content-Type header, or a guess made by the server if it's absent.
	 *
	 * @return True if the content is text, false otherwise.
	 */
	public boolean contentIsText() {
		final String contentType = getContentType();

		return
			contentType.startsWith("text/") ||
			contentType.startsWith(MIME.FORM.getMime()) ||
			contentType.startsWith(MIME.APPLICATION_XML.getMime())
		;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(getMethod().name()).append(' ').append(getResourceChain())
				.append(' ').append(getHttpVersion()).append("\r\n");

		for (Map.Entry<String, String> param : getHeaderParameters().entrySet()) {
			sb.append(param.getKey()).append(": ").append(param.getValue()).append("\r\n");
		}

		if (getContentLength() > 0) {
			sb.append("\r\n").append(getContent());
		}

		return sb.toString();
	}

	/**
	 * Checks whether the specified resource chain is valid for a HTTP request,
	 * taking into account the request method and the scope of the project.
	 *
	 * @param uri The URI to check.
	 * @return True if and only if the resource chain is valid, false in other case.
	 */
	private boolean isValidResourceChain(final String uri) {
		// Start by checking that the URI is not an asterisk with an unsupported method
		boolean toret = !uri.equals("*")
			|| method == HTTPRequestMethod.OPTIONS
			|| method == HTTPRequestMethod.TRACE
		;

		// We only need to consider as valid URIs which contain
		// a relative path. Absolute URIs and authorities handling
		// are out of scope for this project. This is a deviation
		// from the standards
		if (toret) {
			toret = RELATIVE_URI_REGEX.matcher(uri).matches();
		}

		return toret;
	}

	/**
	 * Parses the resource parameters contained in a HTTP request. The source of the
	 * resource parameters varies depending on the method. These parameters are
	 * key-value pairs, where the key is separated from the value with =, and the
	 * pairs separated by &.
	 *
	 * @param resourceParameters The resource parameters to parse.
	 * @param pairsMap           The map to put the parsed key-value pairs on.
	 * @param urlEncoded         Whether the key and the value are encoded in the
	 *                           application/x-www-form-urlencoded format.
	 * @throws HTTPParseException If some key-value pair is malformed.
	 * @return The {@code resourceParameters} argument, but with the keys and values
	 *         decoded.
	 */
	private String parseResourceParameters(final String resourceParameters, final Map<String, String> pairsMap, final boolean urlEncoded) throws HTTPParseException {
		final StringBuilder decodedResourceParameters = new StringBuilder(resourceParameters.length());

		if (!resourceParameters.isEmpty()) {
			for (final String pair : resourceParameters.split("&")) {
				final String[] pairArr = pair.split("=", 2);

				if (pairArr.length != 2) {
					throw new HTTPParseException("Malformed key-value resource parameter in HTTP request");
				}

				String decodedKey, decodedValue;
				if (urlEncoded) {
					try {
						decodedKey = URLDecoder.decode(pairArr[0], StandardCharsets.UTF_8.name());
						decodedValue = URLDecoder.decode(pairArr[1], StandardCharsets.UTF_8.name());
					} catch (final UnsupportedEncodingException exc) {
						// UTF-8 is always available as per Java specification
						throw new AssertionError(exc);
					}
				} else {
					decodedKey = pairArr[0];
					decodedValue = pairArr[1];
				}

				pairsMap.put(decodedKey, decodedValue);
				decodedResourceParameters
					.append(decodedKey)
					.append("=")
					.append(decodedValue)
					.append("&")
				;
			}
		}

		// Get rid of the last '&' if the parameters are not empty
		if (decodedResourceParameters.length() > 0) {
			decodedResourceParameters.setLength(decodedResourceParameters.length() - 1);
		}

		return decodedResourceParameters.toString();
	}

	/**
	 * Retrieves the character encoding used by the client to encode the message
	 * body text, if any was specified.
	 *
	 * @return The described character encoding, which can be {@code null} if no
	 *         character encoding was indicated or the message body is not text.
	 */
	private Charset getBodyCharset() {
		final String[] typeAndParameters = getContentType().split(";");
		Charset bodyCharset = null;

		// Nothing to do if the body is not text
		if (contentIsText()) {
			// Try to get the charset from the Content-Type header, which is the standard way
			boolean foundCharsetParameter = false;
			for (int i = 1; i < typeAndParameters.length && (bodyCharset == null || !foundCharsetParameter); ++i) {
				final String[] parameterPair = typeAndParameters[i].split("=", 2);
				foundCharsetParameter = parameterPair[0].equals("charset");
				if (foundCharsetParameter) {
					try {
						bodyCharset = Charset.forName(parameterPair[1]);
					} catch (UnsupportedCharsetException | IllegalCharsetNameException exc) {
						// Unrecognized charset, ignore
					}
				}
			}

			// If that failed, try to get it from the Content-Encoding header, which is
			// a strange place to look at for determining this, but some clients
			// put it there
			if (
				bodyCharset == null &&
				headerParameters.containsKey(HTTPHeaders.CONTENT_ENCODING.getHeader())
			) {
				try {
					bodyCharset = Charset.forName(
						headerParameters.get(HTTPHeaders.CONTENT_ENCODING.getHeader())
					);
				} catch (UnsupportedCharsetException | IllegalCharsetNameException exc) {
					// Unrecognized charset, ignore
				}
			}

			if (bodyCharset == null) {
				// The RFC says:
				// "When no explicit charset parameter is provided by the sender, media
				// subtypes of the "text" type are defined to have a default charset value
				// of "ISO-8859-1" when received via HTTP."
				// We extend that to any textual content type, not just those who are a
				// subtype of text, for test and real usage compliance
				bodyCharset = StandardCharsets.ISO_8859_1;
			}
		}

		return bodyCharset;
	}

	/**
	 * Returns the content type specified in the Content-Type header by the client,
	 * or a guess made by this server if no type was specified.
	 *
	 * @return The described content type.
	 */
	private String getContentType() {
		// "If and only if the media type is not given by a Content-Type field, the
		// recipient MAY attempt to guess the media type via inspection of its
		// content and/or the name extension(s) of the URI used to identify the
		// resource."
		// For this application textual URI resources are expected, and although
		// our URIs don't have name extensions this is expected to be a good guess
		return headerParameters.getOrDefault(
			HTTPHeaders.CONTENT_TYPE.getHeader(), DEFAULT_CONTENT_TYPE
		);
	}

	/**
	 * Checks whether the specified HTTP version request is supported by this
	 * server.
	 *
	 * @param version The HTTP version to check.
	 * @return True if and only if the HTTP version is supported by HybridServer,
	 *         false if it's not.
	 */
	private static final boolean isSupportedHttpVersion(final String version) {
		// "The <minor> number is incremented when the changes made to the
		// protocol add features which do not change the general message parsing
		// algorithm, but which may add to the message semantics and imply
		// additional capabilities of the sender. The <major> number is
		// incremented when the format of a message within the protocol is
		// changed."
		// That means that, for the purposes of this object, we can accept
		// messages of any HTTP 1.x version
		return version.matches("^HTTP/1\\.[0-9]+$");
	}
}
