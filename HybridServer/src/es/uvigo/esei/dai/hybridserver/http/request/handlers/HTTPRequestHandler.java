package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.ResourceReader;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Defines the contract that any HTTP request handler must obey.
 * <p>
 * To facilitate the creation of a chain of responsibility with HTTP request
 * handlers, it is <b>mandatory</b> that all the implementations of this class
 * define a constructor with two parameters: a {@link HTTPRequest} and a
 * {@link HTTPRequestHandler}, in that order.
 * <p>
 * Also, <b>no HTTP request handler can throw exceptions</b>, no matter if they
 * are checked or unchecked, during the handling of a request.
 *
 * @author Alejandro González García
 */
public abstract class HTTPRequestHandler {
	protected final HTTPRequest request;
	protected final HTTPRequestHandler nextHandler;

	/**
	 * Initializes the HTTP request this handler is associated to, if any.
	 *
	 * @param request     The request to attach to the handler. It may be null, but
	 *                    subclasses might require it to be not null. If it is not
	 *                    null, then the request must be attached to a server.
	 * @param nextHandler The next handler in the responsibility chain, to delegate
	 *                    the responsibility of handling the request if this one is
	 *                    not appropriate. It may be null if there are no more
	 *                    handlers.
	 * @throws IllegalArgumentException If the request is not null, and not
	 *                                  associated to any server.
	 */
	protected HTTPRequestHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		if (request != null && request.getServer() == null) {
			throw new IllegalArgumentException("Can't create a handler for a request not associated to any server");
		}

		this.request = request;
		this.nextHandler = nextHandler;
	}

	/**
	 * Returns the HTTP request handled by this handler. It may be null if this
	 * handler isn't associated to any request (i.e. it is a generic handler).
	 *
	 * @return The described HTTP request.
	 */
	public final HTTPRequest getRequest() {
		return request;
	}

	/**
	 * Handles the HTTP request associated to this object, generating the HTTP
	 * response to send back to the client.
	 *
	 * @return The HTTP response to send back to the client.
	 */
	public final HTTPResponse handleRequest() {
		HTTPResponse response;

		// If we can handle this, do it. Otherwise, delegate
		// to the next handler in the chain
		if (handlesRequest()) {
			response = getResponse();

			if (request != null) {
				request.getServer().getLogger().log(Level.FINE,
					"Generated response {0} for incoming {1} request to {2}",
					new Object[] {
						response.getStatus(),
						request.getMethod(),
						request.getResourceChain()
					}
				);
			}
		} else {
			if (nextHandler != null) {
				response = nextHandler.getResponse();
			} else {
				// This shouldn't happen
				if (request != null) {
					request.getServer().getLogger().log(Level.SEVERE, "No handler found for request. This is a programming error!");
				}

				throw new AssertionError("No handler found for a request: " + request);
			}
		}

		return response;
	}

	@Override
	public final String toString() {
		return this.getClass().getSimpleName();
	}

	/**
	 * Checks whether this handler is able to handle its associated HTTP request. If
	 * it is able to handle the request, it will be the only handler which handles
	 * it. Otherwise, it will delegate the handling to the next handler, until a
	 * suitable handler is found or there are no more handlers (but this last
	 * circumstance should be treated as an implementation error).
	 *
	 * @return True if and only if the handler handles the associated HTTP request,
	 *         false otherwise.
	 */
	protected abstract boolean handlesRequest();

	/**
	 * Retrieves the actual response to a HTTP request. When this method is invoked,
	 * it is guaranteed that this handler is the appropriate one for handling the
	 * request.
	 *
	 * @return The HTTP response to send back to the client. It must be not null.
	 */
	protected abstract HTTPResponse getResponse();

	/**
	 * Convenience method to create an appropriate error status code response to
	 * send back to the client, considering the presence or absence of necessary
	 * server resources.
	 *
	 * @param serverResources The server resources to try to fetch a status code
	 *                        page from. It can be null.
	 * @param status          The error status code of the response.
	 * @return The created HTTP response.
	 * @throws IllegalArgumentException If {@code status} is null.
	 */
	protected final HTTPResponse statusCodeResponse(final ResourceReader serverResources, final HTTPResponseStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Can't create a status code response for a null status code");
		}

		final HTTPResponse toret = new HTTPResponse()
			.setStatus(status)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader());

		final String statusHtml = serverResources == null ? null : serverResources.readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/status_code.htm");

		if (statusHtml != null) {
			toret.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(statusHtml
					.replace("-- STATUS CODE --", Integer.toString(status.getCode()))
					.replace("-- STATUS MESSAGE --", status.getStatus())
				);
		}

		return toret;
	}
}
