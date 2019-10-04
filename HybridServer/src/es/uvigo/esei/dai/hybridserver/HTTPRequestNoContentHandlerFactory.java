package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

final class HTTPRequestNoContentHandlerFactory extends HTTPRequestHandlerFactory {
	@Override
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest httpRequest) {
		return new HTTPRequestNoContentHandler();
	}
}
