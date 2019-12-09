package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresource.HTMLWebResource;

/**
 * Handles (generates) the appropriate response for HTML web resource HTTP GET
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestHTMLWebResourceHandler extends HTTPGETRequestWebResourceHandler<HTMLWebResource> {
	/**
	 * Constructs a new HTTP GET request HTML web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPGETRequestHTMLWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);
	}

	@Override
	protected String getHandledResourceName() {
		return "html";
	}

	@Override
	protected Class<HTMLWebResource> getWebResourceType() {
		return HTMLWebResource.class;
	}
}