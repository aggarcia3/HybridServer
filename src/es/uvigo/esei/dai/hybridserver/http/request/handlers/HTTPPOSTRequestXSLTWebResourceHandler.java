package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresource.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;

/**
 * Handles (generates) the appropriate response for XSLT web resource HTTP POST
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPPOSTRequestXSLTWebResourceHandler extends HTTPPOSTRequestWebResourceHandler<XSLTWebResource> {
	/**
	 * Constructs a new HTTP POST request XSLT web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPPOSTRequestXSLTWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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

	@Override
	protected WebResourcePOSTResult<XSLTWebResource> getPostedWebResource(final UUID resourceUuid, final String resourceContent) {
		XSLTWebResource postedXslt = null;
		HTTPResponseStatus responseStatus = HTTPResponseStatus.S200;

		// Try to get the XSD UUID
		UUID xsdUuid = null;
		try {
			final String xsdUuidString = request.getResourceParameters().get("xsd");

			if (xsdUuidString == null) {
				throw new IllegalArgumentException();
			}

			xsdUuid = UUID.fromString(xsdUuidString);
		} catch (final IllegalArgumentException exc) {
			// UUID not specified or invalid
			responseStatus = HTTPResponseStatus.S400;
		}

		// Validate the specified XSD UUID
		if (xsdUuid != null) {
			try {
				if (request.getServer().getWebResourceDAO(XSDWebResource.class).get(xsdUuid) != null) {
					postedXslt = new XSLTWebResource(resourceUuid, resourceContent, xsdUuid);
				} else {
					// Inexistent XSD
					responseStatus = HTTPResponseStatus.S404;
				}
			} catch (final IOException exc) {
				responseStatus = HTTPResponseStatus.S500;
			}
		}

		return new WebResourcePOSTResult<>(postedXslt, responseStatus);
	}
}
