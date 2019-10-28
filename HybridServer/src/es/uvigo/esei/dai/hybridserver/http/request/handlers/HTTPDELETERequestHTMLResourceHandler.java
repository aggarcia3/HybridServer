package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.util.Map;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresource.IOBackedWebResourceMap;
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceType;

final class HTTPDELETERequestHTMLResourceHandler extends HTTPRequestHandler {

	public HTTPDELETERequestHTMLResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	public boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.DELETE && "html".equals(request.getResourceName());
	}

	@Override
	protected HTTPResponse getResponse() {
		HTTPResponse response;

		try {
			final Map<String, String> queryParameters = request.getResourceParameters();
			final IOBackedWebResourceMap<String, WebResource> htmlResources = request.getServer().getWebResourceMap(WebResourceType.HTML);

			if (!queryParameters.containsKey("uuid")) {
				// No UUID given, so show the list of HTML resources
				response = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S400);
			} else {
				final String requestedUuid = queryParameters.get("uuid");
				final WebResource requestedHtmlResource = htmlResources.remove(requestedUuid);

				if (requestedHtmlResource != null) {
					response = new HTTPResponse()
						.setStatus(HTTPResponseStatus.S200)
						.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
						.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
						.setContent(" <!DOCTYPE html>\n" 
								+ "       <html lang=\"en\">\n" 
								+ "           <head></head>"
								+ "			 <body>"
								+ "				<p>Elemento con uuid "+requestedUuid+" eliminado con éxito</p>"
								+ "			 </body>"
								+ "		 </html>");
				} else {
					// The client wants to get an inexistent HTML resource
					response = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S404);
				}
			}
		} catch (final Exception exc) {
			final HybridServer server = request.getServer();
			if (server != null) {
				server.getLogger().log(Level.WARNING, "An exception has occured while handling a HTML resource DELETE request", exc);
			}

			response = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S500);
		}

		return response;
	}
	


}
