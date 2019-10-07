package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.util.IOBackedMap;

/**
 * Handles (generates) the appropriate response for HTML resource HTTP requests.
 *
 * @author Alejandro González García
 */
final class HTTPRequestHTMLResourceHandler extends HTTPRequestHandler {
	private final String statusHtml;
	private final String listHtml;

	/**
	 * Constructs a new HTTP request welcome page handler.
	 *
	 * @param request The request to associate this handler to.
	 */
	public HTTPRequestHTMLResourceHandler(final HTTPRequest request) {
		super(request);
		this.statusHtml = request.getServer().getResourceReader().readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/status_code.htm");
		this.listHtml = request.getServer().getResourceReader().readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/html_res_list.htm");
	}

	@Override
	public HTTPResponse handle() {
		HTTPResponse toret;

		try {
			final Map<String, String> queryParameters = request.getResourceParameters();
			final IOBackedMap<String, String> htmlResources = request.getServer().getHtmlResourceMap();

			if (!queryParameters.containsKey("uuid")) {
				// No UUID given, so show the list of HTML resources
				toret = htmlResourcesListResponse();
			} else {
				final String requestedUuid = queryParameters.get("uuid");
				final String requestedHtmlResource = htmlResources.get(requestedUuid);

				if (requestedHtmlResource != null) {
					toret = new HTTPResponse()
						.setStatus(HTTPResponseStatus.S200)
						.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
						.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
						.setContent(requestedHtmlResource);
				} else {
					// The client wants to get an inexistent HTML resource
					toret = statusCodeResponse(HTTPResponseStatus.S404);
				}
			}
		} catch (final Exception exc) {
			final HybridServer server = request.getServer();
			if (server != null) {
				server.getLogger().log(Level.WARNING, "An exception has been thrown has occured while handling a HTML resource request", exc);
			}

			toret = statusCodeResponse(HTTPResponseStatus.S500);
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
		final Set<String> htmlResources = request.getServer().getHtmlResourceMap().keySet();
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

		return toret.setContent(String.format(listHtml, resourceListBuilder.toString()));
	}

	/**
	 * Creates an appropriate error status code response, considering the presence or
	 * absence of necessary server resources.
	 *
	 * @return The created HTTP response.
	 */
	private HTTPResponse statusCodeResponse(final HTTPResponseStatus status) {
		final HTTPResponse toret = new HTTPResponse()
			.setStatus(status)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader());

		if (statusHtml != null) {
			toret.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(String.format(statusHtml, status.getCode(), status.getStatus()));
		}

		return toret;
	}
}
