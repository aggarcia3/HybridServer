package es.uvigo.esei.dai.hybridserver.http;

/**
 * Represents an exception which occurs when dealing with an unsupported HTTP
 * header.
 *
 * @author Alejandro González García
 *
 */
public final class HTTPUnsupportedHeaderException extends Exception {
	private static final long serialVersionUID = 1L;

	private final HTTPHeaders header;

	public HTTPUnsupportedHeaderException(HTTPHeaders header) {
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