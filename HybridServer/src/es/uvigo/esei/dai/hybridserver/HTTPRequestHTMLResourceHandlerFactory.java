package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request HTML resource handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestHTMLResourceHandlerFactory extends HTTPRequestHandlerFactory {
	// Initialization-on-demand holder idiom
	private static final class HTTPRequestHTMLResourceHandlerFactoryInstanceHolder {
		static final HTTPRequestHTMLResourceHandlerFactory INSTANCE = new HTTPRequestHTMLResourceHandlerFactory();
	}

	private HTTPRequestHTMLResourceHandlerFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HTTPRequestHTMLResourceHandlerFactory get() {
		return HTTPRequestHTMLResourceHandlerFactoryInstanceHolder.INSTANCE;
	}

	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest request) {
		return new HTTPRequestBadRequestHandler();
	}
}
