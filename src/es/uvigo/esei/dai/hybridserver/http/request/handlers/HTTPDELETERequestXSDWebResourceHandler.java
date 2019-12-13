package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAO;
import es.uvigo.esei.dai.hybridserver.webresource.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;

/**
 * Handles (generates) the appropriate response for XSD web resource HTTP DELETE
 * requests.
 *
 * @author Alejandro González García
 */
final class HTTPDELETERequestXSDWebResourceHandler extends HTTPDELETERequestWebResourceHandler<XSDWebResource> {
	/**
	 * Constructs a new HTTP DELETE request XSD web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	public HTTPDELETERequestXSDWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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
	protected boolean processDeletion(final UUID xsdResourceUuid) {
		final String xsdStringUuid = xsdResourceUuid.toString();
		final WebResourceDAO<XSLTWebResource> dao = request.getServer().getWebResourceDAO(XSLTWebResource.class);
		boolean success = true;

		// FIXME: this algorithm is O(n). A more specific DAO method can reduce its
		// complexity to O(1)
		try {
			final Collection<XSLTWebResource> xsltWebResources = dao.webResources();

			for (final XSLTWebResource xslt : xsltWebResources) {
				final String targetXsdUuid = xslt.getAttribute(XSLTWebResource.XSD_ATTRIBUTE);

				// Check if this XSLT refers to this XSD
				if (xsdStringUuid.equals(targetXsdUuid)) {
					// Transactions don't scale with P2P, so there's not much point in grouping
					// delete operations anyway
					try {
						dao.remove(UUID.fromString(targetXsdUuid));
					} catch (final IOException exc) {
						// Catch the exception here so the loop continues, and as much XSLTs are
						// deleted as possible
						success = false;
					}
				}
			}
		} catch (final IOException exc) {
			success = false;
		}

		return success;
	}
}
