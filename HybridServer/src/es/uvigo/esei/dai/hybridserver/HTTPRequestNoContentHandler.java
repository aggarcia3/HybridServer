package es.uvigo.esei.dai.hybridserver;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;

/**
 * Handles (generates) an empty, 204 status response for any request.
 *
 * @author Alejandro González García
 */
public final class HTTPRequestNoContentHandler extends HTTPRequestHandler {
	@Override
	public HTTPResponse handle() {
		return new HTTPResponse()
				.setStatus(HTTPResponseStatus.S204)
				.setVersion(HTTPHeaders.HTTP_1_1.getHeader());
	}
}
