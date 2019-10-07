package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request welcome page handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestWelcomePageHandlerFactory extends HTTPRequestHandlerFactory {
	// Initialization-on-demand holder idiom
	private static final class HTTPRequestWelcomePageHandlerFactoryInstanceHolder {
		static final HTTPRequestWelcomePageHandlerFactory INSTANCE = new HTTPRequestWelcomePageHandlerFactory();
	}

	private HTTPRequestWelcomePageHandlerFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HTTPRequestWelcomePageHandlerFactory get() {
		return HTTPRequestWelcomePageHandlerFactoryInstanceHolder.INSTANCE;
	}

	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest httpRequest) {
		return new HTTPRequestWelcomePageHandler(httpRequest);
	}
}
