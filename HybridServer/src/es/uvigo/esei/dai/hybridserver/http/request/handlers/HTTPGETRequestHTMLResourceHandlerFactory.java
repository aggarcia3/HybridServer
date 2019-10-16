package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request HTML resource handler.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestHTMLResourceHandlerFactory extends HTTPRequestHandlerFactory {
	// Initialization-on-demand holder idiom
	private static final class HTTPGETRequestHTMLResourceHandlerFactoryInstanceHolder {
		static final HTTPGETRequestHTMLResourceHandlerFactory INSTANCE = new HTTPGETRequestHTMLResourceHandlerFactory();
	}

	private HTTPGETRequestHTMLResourceHandlerFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HTTPGETRequestHTMLResourceHandlerFactory get() {
		return HTTPGETRequestHTMLResourceHandlerFactoryInstanceHolder.INSTANCE;
	}

	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest request) {
		return new HTTPGETRequestHTMLResourceHandler(request);
	}
}
