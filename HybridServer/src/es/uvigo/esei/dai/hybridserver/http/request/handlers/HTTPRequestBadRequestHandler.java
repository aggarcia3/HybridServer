package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Handles (generates) the appropiate response for bad HTTP requests
 * for invalid resources.
 *
 * @author Alejandro González García
 */
final class HTTPRequestBadRequestHandler extends HTTPRequestHandler {
	private final String html;

	/**
	 * Constructs a new HTTP bad request handler.
	 *
	 * @param request The request to associate this handler to.
	 */
	public HTTPRequestBadRequestHandler(final HTTPRequest request) {
		super(request);
		this.html = request.getServer().getResourceReader().readTextResourceToString("/es/uvigo/esei/dai/hybridserver/resources/status_code.htm");
	}

	@Override
	public HTTPResponse handle() {
		final HTTPResponse toret = new HTTPResponse()
			.setStatus(HTTPResponseStatus.S400)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader());

		if (html != null) {
			toret.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en")
				.setContent(html
					.replace("-- STATUS CODE --", Integer.toString(HTTPResponseStatus.S400.getCode()))
					.replace("-- STATUS MESSAGE --", HTTPResponseStatus.S400.getStatus() + ": this server doesn't serve that kind of resource")
				);
		}

		return toret;
	}

	@Override
	public String toString() {
		return "Bad request handler";
	}
}
