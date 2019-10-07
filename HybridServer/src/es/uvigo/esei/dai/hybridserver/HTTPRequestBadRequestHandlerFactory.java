package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Constructs a HTTP request bad request handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestBadRequestHandlerFactory extends HTTPRequestHandlerFactory {
	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest request) {
		return new HTTPRequestBadRequestHandler();
	}
}
