package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresources.XSDWebResource;

/**
 * Handles (generates) the appropriate response for XSD web resource HTTP GET
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestXSDWebResourceHandler extends HTTPGETRequestWebResourceHandler<XSDWebResource> {
	/**
	 * Constructs a new HTTP GET request XSD web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPGETRequestXSDWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);
	}

	@Override
	protected String getHandledResourceName() {
		return "xsd";
	}

	@Override
	protected Class<XSDWebResource> getWebResourceType() {
		return XSDWebResource.class;
	}
}
