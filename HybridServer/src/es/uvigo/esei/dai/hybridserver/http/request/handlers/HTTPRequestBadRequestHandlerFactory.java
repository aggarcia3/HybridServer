package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request bad request handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestBadRequestHandlerFactory extends HTTPRequestHandlerFactory {
	// Initialization-on-demand holder idiom
	private static final class HTTPRequestBadRequestHandlerFactoryInstanceHolder {
		static final HTTPRequestBadRequestHandlerFactory INSTANCE = new HTTPRequestBadRequestHandlerFactory();
	}

	private HTTPRequestBadRequestHandlerFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HTTPRequestBadRequestHandlerFactory get() {
		return HTTPRequestBadRequestHandlerFactoryInstanceHolder.INSTANCE;
	}

	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest request) {
		return new HTTPRequestBadRequestHandler(request);
	}
}
