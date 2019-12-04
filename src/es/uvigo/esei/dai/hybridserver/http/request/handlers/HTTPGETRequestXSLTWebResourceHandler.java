package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;

/**
 * Handles (generates) the appropriate response for XSLT web resource HTTP GET
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestXSLTWebResourceHandler extends HTTPGETRequestWebResourceHandler<XSLTWebResource> {
	/**
	 * Constructs a new HTTP GET request XSLT web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPGETRequestXSLTWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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
