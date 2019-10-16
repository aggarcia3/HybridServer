package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;

/**
 * Defines the contract that any HTTP request handler must obey.
 *
 * @author Alejandro González García
 */
public abstract class HTTPRequestHandler {
	protected final HTTPRequest request;

	/**
	 * Initializes the HTTP request this handler is associated to, if any.
	 *
	 * @param request The request to attach to the handler. It may be null. If it is
	 *                not null, then the request must be attached to a server.
	 * @throws IllegalArgumentException If the request is not null, and not
	 *                                  associated to any server.
	 */
	protected HTTPRequestHandler(final HTTPRequest request) {
		if (request != null && request.getServer() == null) {
			throw new IllegalArgumentException("Can't create a handler for a request not associated to any server");
		}

		this.request = request;
	}

	/**
	 * Constructs an HTTP request handler not associated to any request.
	 */
	protected HTTPRequestHandler() {
		this(null);
	}

	/**
	 * Handles the HTTP request associated to this object, generating the HTTP
	 * response to send back to the client.
	 *
	 * @return The HTTP response to send back to the client.
	 */
	public abstract HTTPResponse handle();
}
