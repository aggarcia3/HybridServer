package es.uvigo.esei.dai.hybridserver.http;

/**
 * Models an exception condition which occurs when a request header is expected,
 * but missing.
 *
 * @author Alejandro González García
 *
 */
public final class HTTPMissingHeaderException extends Exception {
	private static final long serialVersionUID = 1L;

	private final HTTPHeaders header;

	public HTTPMissingHeaderException(HTTPHeaders header) {
		this.header = header;
	}

	/**
	 * Obtains the header responsible for the existence of this exception.
	 *
	 * @return The described header.
	 */
	public HTTPHeaders getHeader() {
		return header;
	}
}