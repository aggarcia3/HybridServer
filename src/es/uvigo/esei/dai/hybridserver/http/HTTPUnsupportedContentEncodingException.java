package es.uvigo.esei.dai.hybridserver.http;

/**
 * Models an exception condition which occurs when a request content encoding
 * is not supported.
 *
 * @author Alejandro González García
 *
 */
public final class HTTPUnsupportedContentEncodingException extends Exception {
	private static final long serialVersionUID = 1L;

	private final String encoding;

	public HTTPUnsupportedContentEncodingException(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Obtains the encoding responsible for the existence of this exception.
	 *
	 * @return The described encoding.
	 */
	public String getEncoding() {
		return encoding;
	}
}