package es.uvigo.esei.dai.hybridserver.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Models a HTTP request.
 * @author Alejandro González García
 */
public final class HTTPRequest {
	/**
	 * Compile and save potentially complex regular expression for speed.
	 * This regex is a straightforward translation of the BNF rules for abs_path
	 * in RFC 2396.
	 */
	private static final Pattern ABS_PATH_REGEX = Pattern.compile("^/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F])*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F])*)*(?:/(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F])*(?:;(?:[A-za-z0-9_.!~*'():@&=+$,-]|%[0-9a-fA-F])*)*)*$");

	private final HTTPRequestMethod method;
	private final String resourceChain;
	private final String httpVersion;
	private final String[] resourcePath;

	public HTTPRequest(Reader reader) throws IOException, HTTPParseException {
		BufferedReader inputReader;
		String requestLine;

		if (reader == null) {
			throw new IllegalArgumentException("Can't associate a HTTP request with a null Reader");
		}

		inputReader = new BufferedReader(reader);

		// According to RFC 2616, section 5, a request is defined by
		// the following grammar:
		// Request       = Request-Line
		//                 *(( general-header
		//                  | request-header
		//                  | entity-header ) CRLF)
		//                 CRLF
		//                 [ message-body ]

		// Therefore, start by reading the request line
		requestLine = inputReader.readLine();
		if (requestLine == null) {
			throw new HTTPParseException("Expected the HTTP request line, but reached end of stream");
		}

		// Get and check the correctness of the method, URI and version fields
		final String[] requestLineFields = requestLine.split(" ", 3);
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
			this.resourcePath = resourceChain.split("/");
		} else {
			throw new HTTPParseException("Unsupported or invalid request URI for a HTTP request method");
		}

		if (isSupportedHttpVersion(requestLineFields[2])) {
			this.httpVersion = requestLineFields[2];
		} else {
			throw new HTTPParseException("Unsupported HTTP version for a HTTP request");
		}

		// TODO
		/*boolean parsingHeaders = true;
		while ((requestLine = inputReader.readLine()) != null && parsingHeaders) {
			
		}

		if (parsingHeaders && requestLine == null) {
			throw new HTTPParseException("Expected more lines while reading HTTP request headers");
		}*/
	}

	public HTTPRequestMethod getMethod() {
		return method;
	}

	public String getResourceChain() {
		return resourceChain;
	}

	public String[] getResourcePath() {
		return resourcePath;
	}

	public String getResourceName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, String> getResourceParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public Map<String, String> getHeaderParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContent() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getContentLength() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(this.getMethod().name()).append(' ').append(this.getResourceChain())
				.append(' ').append(this.getHttpVersion()).append("\r\n");

		for (Map.Entry<String, String> param : this.getHeaderParameters().entrySet()) {
			sb.append(param.getKey()).append(": ").append(param.getValue()).append("\r\n");
		}

		if (this.getContentLength() > 0) {
			sb.append("\r\n").append(this.getContent());
		}

		return sb.toString();
	}

	/**
	 * Checks whether the specified HTTP version request is
	 * supported by this server.
	 * @param version The HTTP version to check.
	 * @return True if and only if the HTTP version is supported
	 * by HybridServer, false if it's not.
	 */
	private static final boolean isSupportedHttpVersion(String version) {
		switch (version) {
			case "HTTP/1.1": // HTTPHeaders.HTTP_1_1.getHeader()
				return true;
			default:
				return false;
		}
	}

	/**
	 * Checks whether the specified resource chain is valid
	 * for a HTTP request, taking into account the request method
	 * and the scope of the project.
	 * @param uri The URI to check.
	 * @return True if and only if the resource chain is valid, false
	 * in other case.
	 */
	private boolean isValidResourceChain(String uri) {
		// Start by checking that the URI is not an asterisk with an unsupported method
		boolean toret = !uri.equals("*") ||
				method == HTTPRequestMethod.OPTIONS ||
				method == HTTPRequestMethod.TRACE
		;

		// We only need to consider as valid URIs which contain
		// an absolute path. Absolute URIs and authorities handling
		// are out of scope for this project
		if (toret) {
			toret = ABS_PATH_REGEX.matcher(uri).matches();
		}

		return toret;
	}
}
