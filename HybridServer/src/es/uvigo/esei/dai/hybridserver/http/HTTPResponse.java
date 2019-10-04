package es.uvigo.esei.dai.hybridserver.http;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Models a HTTP response.
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
	 */
	public HTTPResponse setStatus(final HTTPResponseStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Can't associate a null status code to a HTTP response");
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
	 */
	public HTTPResponse setContent(final String content) {
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
	public void print(final Writer writer) throws IOException {
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
		int explicitContentLength = -1;
		for (final Map.Entry<String, String> parameterPair : parameters.entrySet()) {
			final String key = parameterPair.getKey();
			String value = parameterPair.getValue();

			// Check if we're about to send the content length header.
			// We want to make sure it's correct
			if (key.equalsIgnoreCase(HTTPHeaders.CONTENT_LENGTH.getHeader())) {
				try {
					explicitContentLength = Integer.parseUnsignedInt(parameterPair.getValue());
				} catch (final NumberFormatException exc) {
					// Ignore, we want explicitContentLength to keep its initial value
				}

				if ((content == null && explicitContentLength != 0) ||
					(content != null && explicitContentLength != content.length())
				) {
					// The content length header is not valid, so just replace it silently
					value = Integer.toString(explicitContentLength);
				}
			}

			writer.write(key + ": ");
			writer.write(value);
			writer.write("\r\n");
		}

		// If there is a non-empty message body, but we didn't receive
		// any content length header, add it
		if (content != null && !content.isEmpty() && explicitContentLength < 0) {
			writer.write(HTTPHeaders.CONTENT_LENGTH.getHeader() + ": " + content.length() + "\r\n");
		}

		// Write header ending
		writer.write("\r\n");

		// Write content, if any
		if (content != null && !content.isEmpty()) {
			writer.write(content);
		}
	}

	@Override
	public String toString() {
		final StringWriter writer = new StringWriter();

		try {
			print(writer);
		} catch (final IOException e) {
			// This shouldn't happen with StringWriter
			throw new AssertionError();
		}

		return writer.toString();
	}
}
