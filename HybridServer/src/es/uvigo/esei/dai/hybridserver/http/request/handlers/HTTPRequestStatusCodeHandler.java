package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Always generates a static status code response, no matter what request is it
 * associated to. This handler is more useful when it is the last handler in a
 * responsibility chain, as it will handle any request passed to it, as a last
 * resort handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestStatusCodeHandler extends HTTPRequestHandler {
	private final HTTPResponseStatus status;

	/**
	 * Constructs a new static HTTP status code handler that will generate a 400
	 * Bad Request HTTP response.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers. For this particular
	 *                    handler, any next handler will be effectively ignored, as
	 *                    this handler handles every request.
	 */
	public HTTPRequestStatusCodeHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		this(request, nextHandler, HTTPResponseStatus.S400);
	}

	/**
	 * Constructs a new static HTTP status code handler.
	 *
	 * @param request                  The request to associate this handler to.
	 * @param nextHandler              The next handler in the responsibility chain.
	 *                                 May be null if there are no more handlers.
	 *                                 For this particular handler, any next handler
	 *                                 will be effectively ignored, as this handler
	 *                                 handles every request.
	 * @param status                   The status code of the response to generate.
	 * @param IllegalArgumentException If {@code status} is null.
	 */
	public HTTPRequestStatusCodeHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler, final HTTPResponseStatus status) {
		super(request, nextHandler);

		if (status == null) {
			throw new IllegalArgumentException("Can't create a static HTTP status code handler without a status code");
		}

		this.status = status;
	}

	@Override
	public boolean handlesRequest() {
		return true;
	}

	@Override
	public HTTPResponse getResponse() {
		return statusCodeResponse(null, status);
	}
}
