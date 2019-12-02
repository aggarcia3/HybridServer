package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;

/**
 * Implements an abstract HTTP GET request web resource handler, which
 * facilitates implementation of concrete request handlers for retrieving every
 * type of web resource.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of web resources this handler will retrieve.
 */
abstract class HTTPGETRequestWebResourceHandler<T extends WebResource<T>> extends HTTPRequestHandler {
	/**
	 * Constructs a new abstract HTTP GET request web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	protected HTTPGETRequestWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	protected final boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.GET && getHandledResourceName().equals(request.getResourceName());
	}

	@Override
	protected final HTTPResponse getResponse() {
		HTTPResponse response;

		final HybridServer server = request.getServer();
		try {
			final Map<String, String> queryParameters = request.getResourceParameters();

			if (!queryParameters.containsKey("uuid")) {
				// No UUID given, so show the list of resources
				response = resourceListResponse();
			} else {
				try {
					final UUID requestedUuid = UUID.fromString(queryParameters.get("uuid"));
					final T requestedWebResource = server.getWebResourceDAO(getWebResourceType()).get(requestedUuid);

					if (requestedWebResource != null) {
						response = processResponse(requestedWebResource,
							new HTTPResponse()
								.setStatus(HTTPResponseStatus.S200)
								.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
								.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), requestedWebResource.getMimeType() + "; charset=UTF-8")
								.setContent(requestedWebResource.getAttribute(WebResource.CONTENT_ATTRIBUTE))
						);
					} else {
						// The client wants to get an inexistent web resource
						response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
					}
				} catch (final IllegalArgumentException exc) {
					// The client specified a invalid UUID
					response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
				}
			}
		} catch (final Exception exc) {
			server.getLogger().log(
				Level.WARNING,
				"An exception has occured while handling a " + getWebResourceType().getSimpleName() + " web resource GET request",
				exc
			);

			response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
		}

		return response;
	}

	/**
	 * Returns the resource name that this handler will handle when a GET request
	 * arrives to it.
	 *
	 * @return The described resource name. It must be not {@code null}.
	 * @see HTTPRequest#getResourceName
	 */
	protected abstract String getHandledResourceName();

	/**
	 * Retrieves the concrete web resource type on which this handler will perform
	 * data access operations.
	 *
	 * @return A class object that represents the described type. It must be not
	 *         {@code null}.
	 */
	protected abstract Class<T> getWebResourceType();

	/**
	 * Provides an optional extension point for subclasses, allowing them to
	 * customize the response that will be sent to the client after the requested
	 * web resource was retrieved successfully. The returned HTTP response will be
	 * the one that is finally sent to the client. The default implementation of
	 * this method returns the response that it receives, which conceptually is a
	 * no-op.
	 *
	 * @param requestedWebResource The web resource that the client requested.
	 * @param response             An automatically generated response that contains
	 *                             the requested web resource content, with the
	 *                             corresponding headers, status code and MIME type.
	 * @return The HTTP response to send to the client. It can't be {@code null}.
	 */
	protected HTTPResponse processResponse(final T requestedWebResource, final HTTPResponse response) {
		return response;
	}

	/**
	 * Generates a response that contains a list of web resources of a type
	 * available for download and deletion.
	 *
	 * @return The web resources of a type available for download and deletion.
	 * @throws IOException If some I/O error occurs during the generation of the
	 *                     resource list.
	 */
	private HTTPResponse resourceListResponse() throws IOException {
		final String listHtml = request.getServer().getStaticResourceReader().readTextResourceToString(
			"/es/uvigo/esei/dai/hybridserver/resources/res_list.htm"
		);

		if (listHtml == null) {
			throw new IOException(
				"Tried to generate a web resource list, but the needed resource template couldn't be loaded"
			);
		}

		final HTTPResponse response = new HTTPResponse()
			.setStatus(HTTPResponseStatus.S200)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
			.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
			.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en");

		final Set<UUID> webResources = request.getServer().getWebResourceDAO(getWebResourceType()).uuidSet();
		final StringBuilder resourceListBuilder = new StringBuilder();

		// Generate the HTML code of the list, to substitute on the template
		if (webResources.isEmpty()) {
			resourceListBuilder.append("<p style=\"font-style: italic;\">No resources available.<p>");
		} else {
			resourceListBuilder.append("<ul>\n");
			for (final UUID webResourceUuid : webResources) {
				resourceListBuilder.append("\t<li><a href=\"")
					.append(getHandledResourceName())
					.append("?uuid=")
					.append(webResourceUuid)
					.append("\">")
					.append(webResourceUuid)
					.append("</a></li>\n");
			}
			resourceListBuilder.append("</ul>");
		}

		return response.setContent(listHtml
			.replace("-- RESOURCE LIST PLACEHOLDER --", resourceListBuilder.toString())
		);
	}
}
