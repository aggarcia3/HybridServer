package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Handles (generates) the appropiate response for welcome page HTTP requests.
 *
 * @author Alejandro González García
 */
final class HTTPRequestWelcomePageHandler extends HTTPRequestHandler {
	// This handler just outputs a static HTML page, read from the server resources
	private static final String HTML = ResourceReader.readTextResourceToString(HTTPRequestWelcomePageHandler.class, "/es/uvigo/esei/dai/hybridserver/resources/welcome.htm");

	@Override
	public HTTPResponse handle() {
		return new HTTPResponse()
				.setStatus(HTTPResponseStatus.S200)
				.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
				.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(HTML);
	}

	@Override
	public String toString() {
		return "Welcome page handler";
	}
}
