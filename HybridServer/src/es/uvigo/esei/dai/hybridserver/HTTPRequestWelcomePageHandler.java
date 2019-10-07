package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Handles (generates) the appropiate response for welcome page HTTP requests.
 *
 * @author Alejandro González García
 */
final class HTTPRequestWelcomePageHandler extends HTTPRequestHandler {
	private final String html;

	/**
	 * Constructs a new HTTP welcome page request handler.
	 *
	 * @param request The request to associate this handler to.
	 */
	public HTTPRequestWelcomePageHandler(final HTTPRequest request) {
		super(request);
		this.html = request.getServer().getResourceReader().readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/welcome.htm");
	}

	@Override
	public HTTPResponse handle() {
		if (html != null) {
			return new HTTPResponse()
				.setStatus(HTTPResponseStatus.S200)
				.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
				.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(html);
		} else {
			// No HTML to send because an internal error occurred, so send a 500 status code
			return new HTTPResponse()
				.setStatus(HTTPResponseStatus.S500)
				.setVersion(HTTPHeaders.HTTP_1_1.getHeader());
		}
	}

	@Override
	public String toString() {
		return "Welcome page handler";
	}
}
