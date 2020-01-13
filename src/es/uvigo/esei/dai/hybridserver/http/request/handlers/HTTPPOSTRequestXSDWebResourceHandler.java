package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresources.XSDWebResource;

/**
 * Handles (generates) the appropriate response for XSD web resource HTTP POST
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPPOSTRequestXSDWebResourceHandler extends HTTPPOSTRequestWebResourceHandler<XSDWebResource> {
	/**
	 * Constructs a new HTTP POST request XSD web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPPOSTRequestXSDWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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

	@Override
	protected WebResourcePOSTResult<XSDWebResource> getPostedWebResource(final UUID resourceUuid, final String resourceContent) {
		return new WebResourcePOSTResult<>(
			new XSDWebResource(resourceUuid, resourceContent),
			HTTPResponseStatus.S200
		);
	}
}
