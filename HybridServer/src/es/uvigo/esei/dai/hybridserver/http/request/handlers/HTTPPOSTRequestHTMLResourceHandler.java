package es.uvigo.esei.dai.hybridserver.http.request.handlers;


import java.util.Map;
import java.util.UUID;
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

final class HTTPPOSTRequestHTMLResourceHandler extends HTTPRequestHandler {
	/**
	 * Constructs a new HTTP request welcome page handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is null.
	 */
	public HTTPPOSTRequestHTMLResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
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
	public HTTPResponse getResponse() {
		HTTPResponse response;
		try {
			//Obtain data of the request
			final Map<String,String> data = request.getResourceParameters();
			//Obtains a conection 
			final IOBackedWebResourceMap<String, WebResource> htmlResources = request.getServer().getWebResourceMap(WebResourceType.HTML);
			
			//Obtengo la página pasada para su inserción
			String newPage = data.get("html");
			if(newPage == null) {
				// The client wants to post with an incorrect format
				return statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S400);
			}else {
				//Genera un uuid inicial y comprueba si no existe ya en los datos
				String uuid;
				//En caso de existir genera otro y comprueba
				do {
					uuid = UUID.randomUUID().toString();
				}while(htmlResources.containsKey(uuid));
				//Una vez generado uno no registrado se inserta en el almacenamiento
				
				htmlResources.put(uuid,new WebResource(newPage));
				//Si todo ha salido bien muesrta un enlace a la pagina recién creada
				response = new HTTPResponse()
					.setStatus(HTTPResponseStatus.S200)
					.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
					.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
					.setContent(" <!DOCTYPE html>\n" 
							+ "       <html lang=\"en\">\n" 
							+ "           <head></head>"
							+ "			 <body>"
							+ "				<a href=\"html?uuid="+uuid+"\">"+uuid+"</a>"
							+ "			 </body>"
							+ "		 </html>");
			}
									
		} catch (final Exception exc) {
			final HybridServer server = request.getServer();
			if (server != null) {
				server.getLogger().log(Level.WARNING, "An exception has occured while handling a HTML resource POST request", exc);
			}

			response = statusCodeResponse(request.getServer().getResourceReader(), HTTPResponseStatus.S500);
		}
		return response;
	
	}
	
}
