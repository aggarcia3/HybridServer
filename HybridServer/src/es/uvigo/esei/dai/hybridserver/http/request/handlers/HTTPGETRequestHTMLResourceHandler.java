package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresource.IOBackedWebResourceMap;
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceType;
import es.uvigo.esei.dai.hybridserver.HybridServer;

/**
 * Handles (generates) the appropriate response for HTML resource HTTP requests.
 *
 * @author Alejandro González García
 */
final class HTTPGETRequestHTMLResourceHandler extends HTTPRequestHandler {
	private final String listHtml;

	/**
	 * Constructs a new HTTP request welcome page handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is null.
	 */
	public HTTPGETRequestHTMLResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}

		this.listHtml = request.getServer().getResourceReader().readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/html_res_list.htm");
	}

	@Override
	public boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.GET && "html".equals(request.getResourceName());
	}

	@Override
	public HTTPResponse getResponse() {
		HTTPResponse toret;

		try {
			final Map<String, String> queryParameters = request.getResourceParameters();
			final IOBackedWebResourceMap<String, WebResource> htmlResources = request.getServer().getWebResourceMap(WebResourceType.HTML);

			if (!queryParameters.containsKey("uuid")) {
				// No UUID given, so show the list of HTML resources
				toret = htmlResourcesListResponse();
			} else {
				final String requestedUuid = queryParameters.get("uuid");
				final WebResource requestedHtmlResource = htmlResources.get(requestedUuid);

				if (requestedHtmlResource != null) {
					toret = new HTTPResponse()
						.setStatus(HTTPResponseStatus.S200)
						.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
						.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
						.setContent(requestedHtmlResource.getContent());
				} else {
					// The client wants to get an inexistent HTML resource
					toret = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S404);
				}
			}
		} catch (final Exception exc) {
			final HybridServer server = request.getServer();
			if (server != null) {
				server.getLogger().log(Level.WARNING, "An exception has occured while handling a HTML resource GET request", exc);
			}

			toret = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S500);
		}

		return toret;
	}

	/**
	 * Generates a response that contains a list of HTML resources present in the
	 * server.
	 *
	 * @return The HTML resources present in this server.
	 * @throws IOException If some I/O error occurs during the retrieval of the
	 *                     resource list.
	 */
	private HTTPResponse htmlResourcesListResponse() throws IOException {
		if (listHtml == null) {
			throw new IllegalStateException("Tried to generate a HTML resource list, but the needed resource template couldn't load");
		}

		final HTTPResponse toret = new HTTPResponse()
			.setStatus(HTTPResponseStatus.S200)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
			.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
			.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en");

		final Set<String> htmlResources = request.getServer().getWebResourceMap(WebResourceType.HTML).keySet();
		final StringBuilder resourceListBuilder = new StringBuilder();

		// Generate the HTML code of the list, to substitute on the template
		if (htmlResources.isEmpty()) {
			resourceListBuilder.append("<p style=\"font-style: italic;\">No resources available.<p>");
		} else {
			resourceListBuilder.append("<ul>\n");
			for (String htmlResourceUuid : htmlResources) {
				resourceListBuilder.append("\t<li><a href=\"/html?uuid=");
				resourceListBuilder.append(htmlResourceUuid);
				resourceListBuilder.append("\">");
				resourceListBuilder.append(htmlResourceUuid);
				resourceListBuilder.append("</a></li>\n");
			}
			resourceListBuilder.append("</ul>");
		}

		return toret.setContent(listHtml
			.replace("-- RESOURCE LIST PLACEHOLDER --", resourceListBuilder.toString())
		);
	}
}
