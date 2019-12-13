package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;
import es.uvigo.esei.dai.hybridserver.webresource.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;
import es.uvigo.esei.dai.hybridserver.xml.XSLTHelper;
import es.uvigo.esei.dai.hybridserver.xml.XMLProcessingResult;
import es.uvigo.esei.dai.hybridserver.xml.XSLTTransformResult;

/**
 * Handles (generates) the appropriate response for XML web resource HTTP GET
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestXMLWebResourceHandler extends HTTPGETRequestWebResourceHandler<XMLWebResource> {
	/**
	 * Constructs a new HTTP GET request XML web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPGETRequestXMLWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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
	protected HTTPResponse processResponse(final XMLWebResource xml, final HTTPResponse response) {
		final HybridServer server = request.getServer();
		HTTPResponse actualResponse = response;
		UUID xsltUuid = null;

		// Try to get the XSLT UUID the client could send
		try {
			final String xsltUuidString = request.getResourceParameters().get("xslt");

			if (xsltUuidString == null) {
				throw new IllegalArgumentException(new NullPointerException());
			}

			xsltUuid = UUID.fromString(xsltUuidString);
		} catch (final IllegalArgumentException exc) {
			// Send a Bad Request response if the UUID is provided, but invalid
			if (!(exc.getCause() instanceof NullPointerException)) {
				actualResponse = statusCodeResponse(
					server.getStaticResourceReader(), HTTPResponseStatus.S400
				);
			}
		}

		// Apply the XSLT stylesheet if there is one
		if (xsltUuid != null) {
			try {
				final XSLTWebResource xslt = server.getWebResourceDAO(XSLTWebResource.class).get(xsltUuid);

				if (xslt != null) {
					XSDWebResource xsd;

					try {
						xsd = server.getWebResourceDAO(XSDWebResource.class).get(
							UUID.fromString(xslt.getAttribute(XSLTWebResource.XSD_ATTRIBUTE))
						);
					} catch (final IllegalArgumentException exc) {
						throw new AssertionError("The XSLT has a invalid XSD attribute. This shouldn't happen by contract");
					}

					if (xsd != null) {
						// Mega evolution the XML document :)
						final XSLTTransformResult xsltTransformResult = XSLTHelper.get().transform(xml, xsd, xslt);
						final XMLProcessingResult xsltProcessingResult = xsltTransformResult.getProcessingResult();

						if (xsltProcessingResult.wasSuccessful()) {
							actualResponse = new HTTPResponse()
								.setStatus(HTTPResponseStatus.S200)
								.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
								.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), xsltTransformResult.getMime().getMime())
								.setContent(xsltTransformResult.getContent());
						} else {
							// Generate an error message
							final String html = server.getStaticResourceReader().readTextResourceToString(
								"/es/uvigo/esei/dai/hybridserver/resources/xslt_error_msg.htm"
							);

							if (html != null) {
								actualResponse = new HTTPResponse()
									.setStatus(HTTPResponseStatus.S400)
									.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
									.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime())
									.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
									.setContent(html
										.replace("-- FAILURE REASON --", xsltProcessingResult.getFailureReason())
									);
							} else {
								// Fallback to a not so helpful error message
								actualResponse = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S400);
							}
						}
					} else {
						// The XSD doesn't exist
						actualResponse = statusCodeResponse(
							server.getStaticResourceReader(), HTTPResponseStatus.S404
						);
					}
				} else {
					// The XSLT doesn't exist
					actualResponse = statusCodeResponse(
						server.getStaticResourceReader(), HTTPResponseStatus.S404
					);
				}
			} catch (final IOException exc) {
				actualResponse = statusCodeResponse(
					server.getStaticResourceReader(), HTTPResponseStatus.S500
				);
			}
		}

		return actualResponse;
	}
}
