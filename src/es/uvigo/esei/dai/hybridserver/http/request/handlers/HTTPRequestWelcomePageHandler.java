package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.MIME;

/**
 * Handles (generates) the appropiate response for welcome page HTTP requests.
 *
 * @author Alejandro González García
 */
final class HTTPRequestWelcomePageHandler extends HTTPRequestHandler {
	/**
	 * Constructs a new HTTP welcome page request handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is null.
	 */
	public HTTPRequestWelcomePageHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	public boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.GET && "".equals(request.getResourceName());
	}

	@Override
	public HTTPResponse getResponse() {
		final String html = request.getServer().getStaticResourceReader().readTextResourceToString(
			"/es/uvigo/esei/dai/hybridserver/resources/welcome.htm"
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
			return statusCodeResponse(request.getServer().getStaticResourceReader(), HTTPResponseStatus.S500);
		}
	}
}
