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
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;

/**
 * Implements an abstract HTTP POST request web resource handler, which
 * facilitates implementation of concrete request handlers for creating every
 * type of web resource.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of web resources this handler will create.
 */
abstract class HTTPPOSTRequestWebResourceHandler<T extends WebResource<T>> extends HTTPRequestHandler {
	/**
	 * Constructs a new abstract HTTP POST request web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	protected HTTPPOSTRequestWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	protected boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.POST && getHandledResourceName().equals(request.getResourceName());
	}

	@Override
	protected HTTPResponse getResponse() {
		HTTPResponse response;

		final HybridServer server = request.getServer();
		try {
			final Map<String, String> queryParameters = request.getResourceParameters();
			final String resourceName = getHandledResourceName();

			if (!queryParameters.containsKey(resourceName)) {
				// No POST parameter given, so return a 400 status code response
				response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S400);
			} else {
				final String resourceContent = queryParameters.get(resourceName);
				boolean uuidInUse;
				do {
					final UUID resourceUuid = UUID.randomUUID();
					uuidInUse = false;

					final WebResourcePOSTResult<T> postResult = getPostedWebResource(resourceUuid, resourceContent);
					if (postResult.wasSuccessful()) {
						try {
							server.getWebResourceDAO(getWebResourceType()).put(postResult.getWebResource());

							final String html = server.getStaticResourceReader().readTextResourceToString(
								"/es/uvigo/esei/dai/hybridserver/resources/post_msg.htm"
							);

							if (html != null) {
								response = new HTTPResponse()
									.setStatus(HTTPResponseStatus.S200)
									.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
									.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
									.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
									.setContent(html
										.replace("-- RESOURCE NAME --", resourceName)
										.replace("-- RESOURCE UUID --", resourceUuid.toString())
									);
							} else {
								// No HTML to send because an internal error occurred, so send a 500 status code
								response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
							}
						} catch (final IllegalStateException exc) {
							// According to Wikipedia, the probability to find a duplicate UUID within 103
							// trillion randomly generated UUIDs is one in a billion. But handling that edge
							// case isn't so difficult here, or is it?
							uuidInUse = true;
							response = null; // This value won't be used, but avoids a compiler warning
						}
					} else {
						// Something went wrong while generating the web resource
						response = statusCodeResponse(server.getStaticResourceReader(), postResult.getStatus());
					}
				} while (uuidInUse);
			}
		} catch (final Exception exc) {
			server.getLogger().log(
				Level.WARNING,
				"An exception has occured while handling a " + getWebResourceType().getSimpleName() + " web resource POST request",
				exc
			);

			response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
		}

		return response;
	}

	/**
	 * Returns the resource name that this handler will handle when a POST request
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
	 * Returns the result of trying to instantiate the web resource specified in the
	 * HTTP POST request. The creation can fail because of internal errors in the
	 * server, or because the client didn't fulfill all the requirements for the
	 * request. This method can be invoked several times for a HTTP request, but
	 * with a different UUID.
	 *
	 * @param resourceUuid    The randomly generated UUID that the new web resource
	 *                        must have.
	 * @param resourceContent The content of the web resource, as sent by the client
	 *                        in the corresponding POST parameter.
	 * @return The described result. It can't be {@code null}.
	 */
	protected abstract WebResourcePOSTResult<T> getPostedWebResource(final UUID resourceUuid, final String resourceContent);

	/**
	 * Encapsulates the information related to the result of creating a web resource
	 * for a HTTP POST request.
	 *
	 * @author Alejandro González García
	 *
	 * @param <T> The type of the web resource that is being created.
	 */
	protected static class WebResourcePOSTResult<T> {
		private final T webResource;
		private final HTTPResponseStatus responseStatus;

		/**
		 * Creates a new web resource POST result.
		 *
		 * @param webResource    The web resource represented by the POST request. It is
		 *                       not {@code null} if and only if the the POST result was
		 *                       successful (i.e. the status code is in the range
		 *                       {@code [200, 400)}).
		 * @param responseStatus The HTTP response status deduced from trying to
		 *                       instantiate the specified web resource, that will be
		 *                       sent to the client. It must be not {@code null}.
		 * @throws IllegalArgumentException If any parameter is not valid.
		 */
		protected WebResourcePOSTResult(final T webResource, final HTTPResponseStatus responseStatus) {
			if (responseStatus == null) {
				throw new IllegalArgumentException(
					"A web resource POST result can't have a null response status"
				);
			}
			this.responseStatus = responseStatus;

			if ((wasSuccessful() && webResource == null) || (!wasSuccessful() && webResource != null)) {
				throw new IllegalArgumentException(
					"A web resource POST result can have a associated web resource if and only if it was successful"
				);
			}
			this.webResource = webResource;
		}

		/**
		 * Checks whether the status code of this result indicates success (i.e. the
		 * status code is in the range {@code [200, 400)}).
		 *
		 * @return True if the status code is successful, false otherwise.
		 */
		private boolean wasSuccessful() {
			final int statusCode = getStatus().getCode();
			return statusCode >= 200 && statusCode < 400;
		}

		/**
		 * Returns the HTTP response status deduced from trying to instantiate the
		 * specified web resource, that will be sent to the client.
		 *
		 * @return The described status.
		 */
		private HTTPResponseStatus getStatus() {
			return responseStatus;
		}

		/**
		 * Returns the web resource represented by this request that was instantiated.
		 * This web resource is not {@code null} if and only if this result was
		 * successful.
		 *
		 * @return The aforementioned web resource.
		 */
		private T getWebResource() {
			return webResource;
		}
	}
}
