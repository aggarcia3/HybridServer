package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;

final class HTTPDELETERequestHTMLResourceHandler extends HTTPRequestHandler {

	public HTTPDELETERequestHTMLResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	public boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.POST && "html".equals(request.getResourceName());
	}

	@Override
	protected HTTPResponse getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
	


}
