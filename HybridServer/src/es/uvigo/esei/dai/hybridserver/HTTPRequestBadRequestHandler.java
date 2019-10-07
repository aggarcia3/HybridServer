package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Handles (generates) the appropiate response for bad HTTP requests
 * for invalid resources.
 *
 * @author Alejandro González García
 */
final class HTTPRequestBadRequestHandler extends HTTPRequestHandler {
	// This handler just outputs a static HTML page, read from the server resources
	private static final String HTML = ResourceReader.get().readTextResourceToString(HTTPRequestBadRequestHandler.class, "/es/uvigo/esei/dai/hybridserver/resources/status_code.htm");

	@Override
	public HTTPResponse handle() {
		final HTTPResponse toret = new HTTPResponse()
			.setStatus(HTTPResponseStatus.S400)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader());

		if (HTML != null) {
			toret.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(String.format(HTML, HTTPResponseStatus.S400.getStatus() + ": this server doesn't serve that kind of resource"));
		}

		return toret;
	}

	@Override
	public String toString() {
		return "Bad request handler";
	}
}
