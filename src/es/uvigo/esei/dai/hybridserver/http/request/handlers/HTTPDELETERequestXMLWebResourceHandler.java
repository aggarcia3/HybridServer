package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresource.XMLWebResource;

/**
 * Handles (generates) the appropriate response for XML web resource HTTP DELETE
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPDELETERequestXMLWebResourceHandler extends HTTPDELETERequestWebResourceHandler<XMLWebResource> {
	/**
	 * Constructs a new HTTP DELETE request XML web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPDELETERequestXMLWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);
	}

	@Override
	protected String getHandledResourceName() {
		return "xml";
	}

	@Override
	protected Class<XMLWebResource> getWebResourceType() {
		return XMLWebResource.class;
	}
}
