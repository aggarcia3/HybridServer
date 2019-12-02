package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresource.HTMLWebResource;

/**
 * Handles (generates) the appropriate response for HTML web resource HTTP POST
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPPOSTRequestHTMLWebResourceHandler extends HTTPPOSTRequestWebResourceHandler<HTMLWebResource> {
	/**
	 * Constructs a new HTTP POST request HTML web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPPOSTRequestHTMLWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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

	@Override
	protected WebResourcePOSTResult<HTMLWebResource> getPostedWebResource(final UUID resourceUuid, final String resourceContent) {
		return new WebResourcePOSTResult<>(
			new HTMLWebResource(resourceUuid, resourceContent),
			HTTPResponseStatus.S200
		);
	}
}
