package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;

/**
 * Creates the appropiate HTTP request handlers for HTTP requests.
 *
 * @author Alejandro González García
 */
abstract class HTTPRequestHandlerFactory {
	private static final String NULL_REQUEST_INSTANTIATION_ERROR = "Can't instantiate a HTTP request handler associated with a null HTTP request";

	/**
	 * Creates a HTTP request handler for the specified request.
	 *
	 * @param httpRequest The request to create a HTTP request handler for.
	 * @return The corresponding handler.
	 * @throws IllegalArgumentException If {@code httpRequest} is null.
	 */
	public static HTTPRequestHandler handlerFor(final HTTPRequest httpRequest) {
		HTTPRequestHandler newHandler = null;

		if (httpRequest == null) {
			throw new IllegalArgumentException(NULL_REQUEST_INSTANTIATION_ERROR);
		}

		switch (httpRequest.getMethod()) {
			case GET: {
				// If the client sent a GET request to the root path, we should respond with the welcome page
				if (httpRequest.getResourceName().equals("")) {
					newHandler = new HTTPRequestWelcomePageHandlerFactory().instantiateHandler(httpRequest);
				}

				break;
			}
			default: // Let the default handler take over
		}

		if (newHandler == null) {
			// Send a 400 response
			newHandler = new HTTPRequestBadRequestHandlerFactory().instantiateHandler(httpRequest);
		}

		return newHandler;
	}

	/**
	 * Instantiates a concrete HTTP request handler implementation, and returns it.
	 * The default implementation of this method just checks that the parameter is
	 * not null and returns null if it is not, so subclasses <b>must</b> override
	 * it. Remarkably, subclasses may not require that the request is non-null.
	 * If they impose additional requirements on the HTTP request, they <b>must</b>
	 * document them.
	 *
	 * @param httpRequest The HTTP request to associate to the new handler.
	 * @return The HTTP request handler.
	 * @throws IllegalArgumentException If {@code httpRequest} is null.
	 */
	protected HTTPRequestHandler instantiateHandler(final HTTPRequest httpRequest) {
		if (httpRequest == null) {
			throw new IllegalArgumentException(NULL_REQUEST_INSTANTIATION_ERROR);
		}

		return null;
	}
}
