package es.uvigo.esei.dai.hybridserver.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import es.uvigo.esei.dai.hybridserver.HybridServer;

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
			"^/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*)*(?:/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F]{2})*)*)*(?:\\?(?:[;/?:@&=+$,a-zA-Z0-9_.!~*'()-]|%[0-9a-fA-F]{2})*)?$");

	private static final String BAD_CONTENT_LENGTH_MESSAGE = "The received Content-Length header doesn't match the actual HTTP request body size";

	private final HybridServer server;
	private final HTTPRequestMethod method;
	private final String resourceChain;
	private final String httpVersion;
	private final String[] resourcePath;
	private final String resourceName;
	private final Map<String, String> resourceParameters;
	private final Map<String, String> headerParameters;
	private final String content;
	private final int contentLength;

	/**
	 * Creates a HTTP request object associated to a server by parsing input from a reader.
	 *
	 * @param server The server where this HTTP request arrived.
	 * @param reader The reader to get a HTTP request from.
	 * @throws IOException        If an I/O error occurs during parsing.
	 * @throws HTTPParseException If the HTTP request has a non-conforming syntax,
	 *                            or some characteristic of it prevents it from
	 *                            being processed by this server. Users should check
	 *                            the cause of this exception to know whether it is
	 *                            a bad request or the server just lacks support for
	 *                            it.
	 */
	public HTTPRequest(final HybridServer server, final Reader reader) throws IOException, HTTPParseException {
		BufferedReader inputReader;
		String inputLine;

		if (reader == null) {
			throw new IllegalArgumentException("Can't associate a HTTP request with a null Reader");
		}

		// This field may be null
		this.server = server;

		inputReader = new BufferedReader(reader);

		// According to RFC 2616, section 5, a request is defined by
		// the following grammar:
		// Request =	Request-Line
		// 				*(( general-header
		// 				| request-header
		// 				| entity-header ) CRLF)
		// 				CRLF
		// 				[ message-body ]

		// Therefore, start by reading the request line. The BufferedReader
		// is lenient with the standard, which mandates CRLF line ending,
		// because it also supports LF and CR line endings
		inputLine = inputReader.readLine();
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
				case HEAD: {
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
		long messageBodyLength = -1L;
		final Map<String, String> headerParameters = new LinkedHashMap<>(); // Linked so we preserve ordering

		while (!headersEnd && (inputLine = inputReader.readLine()) != null) {
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
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.CONTENT_ENCODING.getHeader())
						&& !headerPair[1].equalsIgnoreCase("identity")
						&& !headerPair[1].equalsIgnoreCase("UTF-8")
				) {
					throw new HTTPParseException(
							"This server does not support the specified content encoding for HTTP requests",
							new HTTPUnsupportedContentEncodingException(headerPair[1].toLowerCase())
					);
				}

				// Get message body length from the corresponding header
				if (headerPair[0].equalsIgnoreCase(HTTPHeaders.CONTENT_LENGTH.getHeader())) {
					try {
						// The parse method is lenient with the standard, because it allows an
						// extra '+' symbol before the digits
						messageBodyLength = Long.parseUnsignedLong(headerPair[1]);
					} catch (final NumberFormatException exc) {
						throw new HTTPParseException("Invalid Content-Length in HTTP request: expected 64-bit natural number", exc);
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

		if (messageBodyLength < 0 && inputReader.ready() && inputReader.read() > -1) {
			throw new HTTPParseException("Missing Content-Length header for HTTP request",
					new HTTPMissingHeaderException(HTTPHeaders.CONTENT_LENGTH)
			);
		}

		String rawContent;
		boolean rawContentIsDecoded = false;
		if (messageBodyLength > 0) {
			final char[] buf = new char[4 * 1024 * 1024]; // 4 MiB chunk size
			final StringBuilder contentBuilder = new StringBuilder(buf.length);

			// Read the first complete chunks. We don't read the entire
			// body at once to reduce memory consumption and handle
			// content length corruption more gracefully
			for (int i = 0; i < messageBodyLength / buf.length; ++i) {
				if (inputReader.read(buf) != buf.length) {
					throw new HTTPParseException(BAD_CONTENT_LENGTH_MESSAGE);
				}

				contentBuilder.append(buf);
			}

			// Read the last remaining characters, if any
			final int remainingChars = (int) Math.min(messageBodyLength % buf.length, Integer.MAX_VALUE);
			if (remainingChars > 0) {
				if (inputReader.read(buf, 0, remainingChars) != remainingChars) {
					throw new HTTPParseException(BAD_CONTENT_LENGTH_MESSAGE);
				}

				contentBuilder.append(buf, 0, remainingChars);
			}

			rawContent = contentBuilder.toString();
		} else {
			rawContent = null; // Null to distinguish from empty body
		}

		if (rawContent != null) {
			final String contentType = headerParameters.get("Content-Type");

			switch (method) {
				case POST:
				case PUT: {
					// Parse body as resource parameters. The tests require us to follow
					// the same format as for query strings in the URI. Also, use the
					// return value to save iterating over the content again
					final boolean urlEncoded = "application/x-www-form-urlencoded".equals(contentType);
					rawContent = parseResourceParameters(rawContent, resourceParameters, urlEncoded);
					rawContentIsDecoded = urlEncoded;
				}
				default: // Do not parse body as resource parameters
			}

			if (!rawContentIsDecoded && contentType != null) {
				// Decode body depending on type
				switch (contentType) {
					case "application/x-www-form-urlencoded": {
						this.content = URLDecoder.decode(rawContent, "UTF-8");
						break;
					}
					default: {
						// Other content types do not get decoded
						this.content = rawContent;
					}
				}
			} else {
				// Nothing to decode
				this.content = rawContent;
			}
		} else {
			// Null stays like that
			this.content = rawContent;
		}

		// Clamp content length
		this.contentLength = (int) Math.min(Math.max(messageBodyLength, 0), Integer.MAX_VALUE);
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
	 */
	public HTTPRequest(final Reader reader) throws IOException, HTTPParseException {
		this(null, reader);
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
	 * Obtains the full body of this request, sent by the client. If its
	 * Content-Type is recognized, it's automatically decoded to its intended form.
	 *
	 * @return The body of this request. If the client didn't send a request body, a
	 *         null value will be returned.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Returns the length of the body of this request. It always is zero or greater.
	 * The request body might be larger than {@code Integer.MAX_VALUE} characters,
	 * in which case this method returns {@code Integer.MAX_VALUE}. Even if the
	 * request body fits in an integer, the content length may be different to the
	 * actual content size because of the decoding done on the server side.
	 *
	 * @return The length of the body of this request.
	 */
	public int getContentLength() {
		return contentLength;
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
	private boolean isValidResourceChain(String uri) {
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
				final String[] pairArr = pair.split("=");

				if (pairArr.length != 2) {
					throw new HTTPParseException("Malformed key-value resource parameter in HTTP request");
				}

				String decodedKey, decodedValue;
				if (urlEncoded) {
					try {
						decodedKey = URLDecoder.decode(pairArr[0], "UTF-8");
						decodedValue = URLDecoder.decode(pairArr[1], "UTF-8");
					} catch (final UnsupportedEncodingException exc) {
						// UTF-8 is always available as per Java specification
						throw new AssertionError(exc);
					}
				} else {
					decodedKey = pairArr[0];
					decodedValue = pairArr[1];
				}

				pairsMap.put(decodedKey, decodedValue);
				decodedResourceParameters.append(decodedKey).append("=").append(decodedValue).append("&");
			}
		}

		// Get rid of the last '&' if the parameters are not empty
		if (decodedResourceParameters.length() > 0) {
			decodedResourceParameters.setLength(decodedResourceParameters.length() - 1);
		}

		return decodedResourceParameters.toString();
	}

	/**
	 * Checks whether the specified HTTP version request is supported by this
	 * server.
	 *
	 * @param version The HTTP version to check.
	 * @return True if and only if the HTTP version is supported by HybridServer,
	 *         false if it's not.
	 */
	private static final boolean isSupportedHttpVersion(String version) {
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
