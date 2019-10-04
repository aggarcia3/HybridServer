package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request welcome page handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestWelcomePageHandlerFactory extends HTTPRequestHandlerFactory {
	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest httpRequest) {
		super.instantiateHandler(httpRequest);
		return new HTTPRequestWelcomePageHandler(httpRequest);
	}
}
