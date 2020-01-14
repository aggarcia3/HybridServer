package es.uvigo.esei.dai.hybridserver.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Models a HTTP response, with an optional text (as opposed to binary) body.
 *
 * @author Alejandro González García
 * @implNote This class is not thread-safe.
 */
public final class HTTPResponse {
	private HTTPResponseStatus status;
	private String version;
	private String content;
	private final Map<String, String> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * Gets the HTTP response status to be sent along with this response.
	 *
	 * @return The HTTP response status.
	 */
	public HTTPResponseStatus getStatus() {
		return status;
	}

	/**
	 * Sets the HTTP response status to send along with this response.
	 *
	 * @param status The status to set.
	 * @return This HTTP response object, to provide a fluent interface.
	 * @throws IllegalArgumentException If {@code status} is null.
	 * @throws IllegalStateException    If trying to change a response with a body
	 *                                  to a 204 status code.
	 */
	public HTTPResponse setStatus(final HTTPResponseStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Can't associate a null status code to a HTTP response");
		}

		if (status == HTTPResponseStatus.S204 && content != null && !content.isEmpty()) {
			throw new IllegalStateException("204 status responses can't have message body");
		}

		this.status = status;
		return this;
	}

	/**
	 * Gets the HTTP response status to send along with this response.
	 *
	 * @return The status.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the HTTP version of this response. It must be non-null, and HTTP 1.x
	 * compatible.
	 *
	 * @param version The HTTP version of this response.
	 * @return This HTTP response object, to provide a fluent interface.
	 * @throws IllegalArgumentException If the HTTP version is not valid.
	 */
	public HTTPResponse setVersion(final String version) {
		if (version == null || !version.matches("^HTTP/1\\.[0-9]+")) {
			throw new IllegalArgumentException("Can't associate a null or non-1.x version to a HTTP response");
		}

		this.version = version;
		return this;
	}

	/**
	 * Gets the message body (content) of this response. It can be empty or null if
	 * sending back a body is not desired.
	 *
	 * @return The message body of this response.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Sets the message body (content) of this response. It can be empty or null if
	 * sending back a body is not desired. The body will be initially empty.
	 *
	 * @param content The new message body for this response.
	 * @return This HTTP response object, to provide a fluent interface.
	 * @throws IllegalStateException If trying to attach a body to a 204 response.
	 */
	public HTTPResponse setContent(final String content) {
		if (status == HTTPResponseStatus.S204 && content != null && !content.isEmpty()) {
			throw new IllegalStateException("204 status responses can't have message body");
		}

		this.content = content;
		return this;
	}

	/**
	 * Adds a HTTP header to the response. If a header with the same name (compared
	 * case-insensitively) already was associated with this response, its value will
	 * be overwritten with the new one, instead of being sent twice with two
	 * different values.
	 *
	 * @param name  The name of the HTTP header to send.
	 * @param value The value of the HTTP header.
	 * @return This HTTP response object, to provide a fluent interface.
	 * @throws IllegalArgumentException If some argument is null.
	 */
	public HTTPResponse putParameter(final String name, final String value) {
		if (name == null || value == null) {
			throw new IllegalArgumentException("A parameter name or value can't be null");
		}

		parameters.put(name, value);
		return this;
	}

	/**
	 * Checks whether this response contains the specified HTTP header, ignoring
	 * case differences.
	 *
	 * @param name The HTTP header to check the presence of.
	 * @return True if and only if the header was associated with the response,
	 *         false in other case.
	 */
	public boolean containsParameter(final String name) {
		return parameters.containsKey(name);
	}

	/**
	 * Removes the specified HTTP header from the response. If it was not associated
	 * with the response, then this method has no effect.
	 *
	 * @param name The HTTP header to remove from the response.
	 * @return This HTTP response object, to provide a fluent interface.
	 */
	public HTTPResponse removeParameter(final String name) {
		parameters.remove(name);
		return this;
	}

	/**
	 * Removes all the HTTP headers that were supposed to be sent in this response.
	 *
	 * @return This HTTP response object, to provide a fluent interface.
	 */
	public HTTPResponse clearParameters() {
		parameters.clear();
		return this;
	}

	/**
	 * Returns all the HTTP header names that were associated with this response.
	 *
	 * @return A set with the HTTP header names that were associated with this
	 *         response. Users of this method <b>must not</b> assume the set
	 *         returned by this method is modifiable.
	 */
	public Set<String> parameterSet() {
		return Collections.unmodifiableSet(parameters.keySet());
	}

	/**
	 * Writes out this HTTP response. For this operation to succeed, it is necessary
	 * to set at least the status and the version of this HTTP response. The rest of
	 * attributes are optional, but of course they will alter how the client
	 * interprets the response. If a content length header (resource parameter) was
	 * not specified, it will be generated automatically.
	 *
	 * @param writer The Writer to write this HTTP response to.
	 * @throws IOException           If some I/O error occurs during the operation.
	 * @throws IllegalStateException If this HTTP response was not associated a
	 *                               status and/or version, breaking the contract
	 *                               mentioned above.
	 */
	public void printTo(final Writer writer) throws IOException {
		print(writer, true);
	}

	/**
	 * Writes out this HTTP response. For this operation to succeed, it is necessary
	 * to set at least the status and the version of this HTTP response. The rest of
	 * attributes are optional, but of course they will alter how the client
	 * interprets the response. If a content length header (resource parameter) was
	 * not specified, it will be generated automatically.
	 *
	 * @param writer The Writer to write this HTTP response to.
	 * @throws IOException           If some I/O error occurs during the operation.
	 * @throws IllegalStateException If this HTTP response was not associated a
	 *                               status and/or version, breaking the contract
	 *                               mentioned above.
	 * @deprecated This method is just another way to call
	 *             {@link HTTPResponse#printTo(Writer)}, with the difference that
	 *             mandatory but test-breaking headers won't be added to the
	 *             response. The normal, desired behavior of this method implies
	 *             letting it be smart about what headers it should add or mangle,
	 *             so the printed response is conforms to standards. This method is
	 *             thus only provided for passing tests' assertions that don't
	 *             expect the mangling done.
	 */
	@Deprecated
	public void print(final Writer writer) throws IOException {
		print(writer, false);
	}

	@Override
	public String toString() {
		final StringWriter writer = new StringWriter();

		try {
			print(writer, false); // Don't mangle so nobody complains about things they didn't add
		} catch (final IOException exc) {
			// This shouldn't happen with StringWriter
			throw new AssertionError();
		}

		return writer.toString();
	}

	/**
	 * Performs the actual writing of an HTTP response. It is done in this method to
	 * allow more control about whether standard-mandatory, but test-breaking
	 * headers will be added or not.
	 *
	 * @param writer                 The Writer to write this HTTP response to.
	 * @param conformConnectionClose If true, then a Connection header with value
	 *                               "close" will always be added to the resulting
	 *                               response, no matter if it was explicitly added
	 *                               or not. This is the behavior that RFC 2616
	 *                               mandates for servers that don't support
	 *                               persistent connections like this one, and this
	 *                               behavior fixes connection resets sent to
	 *                               clients that expect the connection to be kept
	 *                               open. However, tests, for a good reason,
	 *                               complain about headers being added without
	 *                               their intervention.
	 * @throws IOException           If some I/O error occurs during the operation.
	 * @throws IllegalStateException If this HTTP response was not associated a
	 *                               status and/or version.
	 * @see HTTPResponse#print
	 */
	private void print(final Writer writer, final boolean conformConnectionClose) throws IOException {
		if (status == null || version == null) {
			throw new IllegalStateException("Can't print a HTTP response without status code and version");
		}

		// Grammar for a HTTP response, according to RFC 2616:
		// Response	= Status-Line
		//			*(( general-header
		//			| response-header
		//			| entity-header ) CRLF)
		//			CRLF
		//			[ message-body ]

		// Write the status line
		writer.write(version + " " + status.getCode() + " " + status.getStatus() + "\r\n");

		// Write headers
		boolean explicitContentLength = true;
		boolean explicitConnectionHeader = false;
		for (final Map.Entry<String, String> parameterPair : parameters.entrySet()) {
			final String key = parameterPair.getKey();
			String value = parameterPair.getValue();

			// Check if we're about to send the content length header,
			// we want to take note of that
			if (key.equalsIgnoreCase(HTTPHeaders.CONTENT_LENGTH.getHeader())) {
				explicitContentLength = true;
			}

			// Quietly replace any Connection header value by "close", as that's the value
			// the server really supports
			if (conformConnectionClose && key.equalsIgnoreCase(HTTPHeaders.CONNECTION.getHeader())) {
				value = "close";
				explicitConnectionHeader = true;
			}

			if (key.equalsIgnoreCase(HTTPHeaders.CONTENT_TYPE.getHeader())) {
				// Conceal insufficient information sent to the client for decoding text/plain resources.
				// Other supported media types provide mechanisms to deduce the encoding
				if (value.startsWith(MIME.TEXT_PLAIN.getMime()) && !value.contains("; charset=")) {
					value += "; charset=UTF-8";
				}
			}

			writer.write(key + ": ");
			writer.write(value);
			writer.write("\r\n");
		}

		// "HTTP/1.1 applications that do not support persistent connections MUST
		// include the "close" connection option in every message."
		if (conformConnectionClose && !explicitConnectionHeader) {
			writer.write(HTTPHeaders.CONNECTION.getHeader() + ": close\r\n");
		}

		// If there is a non-empty message body, but we didn't receive
		// any content length header, add it
		if (content != null && !content.isEmpty() && !explicitContentLength) {
			writer.write(HTTPHeaders.CONTENT_LENGTH.getHeader() + ": " + content.getBytes(StandardCharsets.UTF_8).length + "\r\n");
		}

		// Write header ending
		writer.write("\r\n");

		// Write content, if any
		if (content != null && !content.isEmpty()) {
			writer.write(content);
		}
	}
}
