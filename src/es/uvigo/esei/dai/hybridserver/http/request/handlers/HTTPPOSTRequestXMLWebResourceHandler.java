package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresources.XMLWebResource;

/**
 * Handles (generates) the appropriate response for XML web resource HTTP POST
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPPOSTRequestXMLWebResourceHandler extends HTTPPOSTRequestWebResourceHandler<XMLWebResource> {
	/**
	 * Constructs a new HTTP POST request XML web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPPOSTRequestXMLWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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

	@Override
	protected WebResourcePOSTResult<XMLWebResource> getPostedWebResource(final UUID resourceUuid, final String resourceContent) {
		return new WebResourcePOSTResult<>(
			new XMLWebResource(resourceUuid, resourceContent),
			HTTPResponseStatus.S200
		);
	}
}
