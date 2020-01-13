package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;
import es.uvigo.esei.dai.hybridserver.webresources.WebResource;
import es.uvigo.esei.dai.hybridserver.webresources.WebResourceDAO;

/**
 * Implements an abstract HTTP DELETE request web resource handler, which
 * facilitates implementation of concrete request handlers for deleting every
 * type of web resource.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of web resources this handler will delete.
 */
abstract class HTTPDELETERequestWebResourceHandler<T extends WebResource<T>> extends HTTPRequestHandler {
	/**
	 * Constructs a new abstract HTTP DELETE request web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	protected HTTPDELETERequestWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	protected final boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.DELETE && getHandledResourceName().equals(request.getResourceName());
	}

	@Override
	protected final HTTPResponse getResponse() {
		HTTPResponse response;

		final HybridServer server = request.getServer();
		try {
			final Map<String, String> queryParameters = request.getResourceParameters();
			final WebResourceDAO<T> webResourceDao = server.getWebResourceDAO(getWebResourceType());

			if (queryParameters.containsKey("uuid")) {
				try {
					final UUID requestedUuid = UUID.fromString(queryParameters.get("uuid"));

					if (webResourceDao.remove(requestedUuid)) {
						if (processDeletion(requestedUuid)) {
							final String html = server.getStaticResourceReader().readTextResourceToString(
								"/es/uvigo/esei/dai/hybridserver/resources/delete_msg.htm"
							);

							if (html != null) {
								return new HTTPResponse()
									.setStatus(HTTPResponseStatus.S200)
									.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
									.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), MIME.TEXT_HTML.getMime())
									.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
									.setContent(html);
							} else {
								// No HTML to send because an internal error occurred, so send a 500 status code
								response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
							}
						} else {
							// Something went wrong while processing the deletion
							response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
						}
					} else {
						// The client wants to delete an inexistent web resource
						response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
					}
				} catch (final IllegalArgumentException exc) {
					// The client specified a invalid UUID
					response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
				}
			} else {
				// The client did not specify a UUID
				response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S400);
			}
		} catch (final Exception exc) {
			server.getLogger().log(
				Level.WARNING,
				"An exception has occured while handling a " + getWebResourceType().getSimpleName() + " web resource DELETE request",
				exc
			);

			response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
		}

		return response;
	}

	/**
	 * Returns the resource name that this handler will handle when a DELETE request
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
	 * Provides an optional extension point for subclasses, allowing them to do
	 * additional operations after the web resource with the specified UUID was
	 * deleted successfully. If this method returns {@code false}, which signals
	 * that something went wrong during the operation, the handler will emit a HTTP
	 * response with a 500 status code. The default implementation of this method
	 * does nothing except return {@code true}.
	 *
	 * @param webResourceUuid The UUID of the web resource that was just deleted.
	 * @return True if any additional operations were executed successfully, false
	 *         otherwise.
	 */
	protected boolean processDeletion(final UUID webResourceUuid) {
		return true;
	}
}
