package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.StaticResourceReader;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * A handler that always generates a static status code response, no matter what
 * request is it associated to. This handler is more useful when it is the last
 * handler in a responsibility chain, as it will handle any request passed to
 * it, acting like a last resort handler.
 *
 * @author Alejandro González García
 */
final class HTTPRequestStatusCodeHandler extends HTTPRequestHandler {
	private final HTTPResponseStatus status;
	private final StaticResourceReader staticResources;

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
		this(request, nextHandler, HTTPResponseStatus.S400, null);
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
	 * @param staticResources          A static resource reader of the server that
	 *                                 is responsible for the request. This argument
	 *                                 is only used if the request parameter is
	 *                                 null, which happens when the request object
	 *                                 couldn't be generated because of parsing
	 *                                 errors.
	 * @param IllegalArgumentException If {@code status} is null.
	 */
	public HTTPRequestStatusCodeHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler, final HTTPResponseStatus status, final StaticResourceReader staticResources) {
		super(request, nextHandler);

		if (status == null) {
			throw new IllegalArgumentException("Can't create a static HTTP status code handler without a status code");
		}

		this.status = status;
		this.staticResources = staticResources;
	}

	@Override
	public boolean handlesRequest() {
		return true;
	}

	@Override
	public HTTPResponse getResponse() {
		StaticResourceReader serverResources = this.staticResources;

		if (request != null) {
			serverResources = request.getServer().getStaticResourceReader();
		}

		return statusCodeResponse(serverResources, status);
	}
}
