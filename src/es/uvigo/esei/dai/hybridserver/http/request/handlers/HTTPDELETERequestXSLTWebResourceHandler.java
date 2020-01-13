package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresources.XSLTWebResource;

/**
 * Handles (generates) the appropriate response for XSLT web resource HTTP DELETE
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPDELETERequestXSLTWebResourceHandler extends HTTPDELETERequestWebResourceHandler<XSLTWebResource> {
	/**
	 * Constructs a new HTTP DELETE request XSLT web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPDELETERequestXSLTWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);
	}

	@Override
	protected String getHandledResourceName() {
		return "xslt";
	}

	@Override
	protected Class<XSLTWebResource> getWebResourceType() {
		return XSLTWebResource.class;
	}
}
