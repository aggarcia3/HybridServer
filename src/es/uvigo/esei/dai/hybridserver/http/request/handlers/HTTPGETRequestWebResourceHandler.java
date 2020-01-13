package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.ServerConfiguration;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequestMethod;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.webresources.WebResource;
import es.uvigo.esei.dai.hybridserver.webservices.HybridServerWebService;
import es.uvigo.esei.dai.hybridserver.webservices.RemoteServerWebServiceCommunicationController;

/**
 * Implements an abstract HTTP GET request web resource handler, which
 * facilitates implementation of concrete request handlers for retrieving every
 * type of web resource.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of web resources this handler will retrieve.
 */
abstract class HTTPGETRequestWebResourceHandler<T extends WebResource<T>> extends HTTPRequestHandler {
	private static final String NO_RES_AVAILABLE = "<p style=\"font-style: italic;\">No resources available.<p>";

	/**
	 * Constructs a new abstract HTTP GET request web resource handler.
	 *
	 * @param request     The request to associate this handler to.
	 * @param nextHandler The next handler in the responsibility chain. May be null
	 *                    if there are no more handlers.
	 * @throws IllegalArgumentException If the request is {@code null}.
	 */
	protected HTTPGETRequestWebResourceHandler(final HTTPRequest request, final HTTPRequestHandler nextHandler) {
		super(request, nextHandler);

		if (request == null) {
			throw new IllegalArgumentException("A request is needed for this handler");
		}
	}

	@Override
	protected final boolean handlesRequest() {
		return request.getMethod() == HTTPRequestMethod.GET && getHandledResourceName().equals(request.getResourceName());
	}

	@Override
	protected final HTTPResponse getResponse() {
		HTTPResponse response;

		final HybridServer server = request.getServer();
		try {
			final Map<String, String> queryParameters = request.getResourceParameters();

			if (!queryParameters.containsKey("uuid")) {
				// No UUID given, so show the list of resources
				response = resourceListResponse();
			} else {
				try {
					final UUID requestedUuid = UUID.fromString(queryParameters.get("uuid"));
					final T requestedWebResource = server.getWebResourceDAO(getWebResourceType()).get(requestedUuid);

					if (requestedWebResource != null) {
						response = processResponse(requestedWebResource,
							new HTTPResponse()
								.setStatus(HTTPResponseStatus.S200)
								.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
								.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), requestedWebResource.getMimeType())
								.setContent(requestedWebResource.getAttribute(WebResource.CONTENT_ATTRIBUTE))
						);
					} else {
						// The client wants to get an inexistent web resource
						response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
					}
				} catch (final IllegalArgumentException exc) {
					// The client specified a invalid UUID
					response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S404);
				}
			}
		} catch (final Exception exc) {
			server.getLogger().log(
				Level.WARNING,
				"An exception has occured while handling a " + getWebResourceType().getSimpleName() + " web resource GET request",
				exc
			);

			response = statusCodeResponse(server.getStaticResourceReader(), HTTPResponseStatus.S500);
		}

		return response;
	}

	/**
	 * Returns the resource name that this handler will handle when a GET request
	 * arrives to it.
	 *
	 * @return The described resource name. It must be not {@code null}.
	 * @see HTTPRequest#getResourceName
	 */
	protected abstract String getHandledResourceName();

	/**
	 * Retrieves the concrete web resource type on which this handler will perform
	 * data access operations.
	 *
	 * @return A class object that represents the described type. It must be not
	 *         {@code null}.
	 */
	protected abstract Class<T> getWebResourceType();

	/**
	 * Provides an optional extension point for subclasses, allowing them to
	 * customize the response that will be sent to the client after the requested
	 * web resource was retrieved successfully. The returned HTTP response will be
	 * the one that is finally sent to the client. The default implementation of
	 * this method returns the response that it receives, which conceptually is a
	 * no-op.
	 *
	 * @param requestedWebResource The web resource that the client requested.
	 * @param response             An automatically generated response that contains
	 *                             the requested web resource content, with the
	 *                             corresponding headers, status code and MIME type.
	 * @return The HTTP response to send to the client. It can't be {@code null}.
	 */
	protected HTTPResponse processResponse(final T requestedWebResource, final HTTPResponse response) {
		return response;
	}

	/**
	 * Generates a response that contains a list of web resources of a type
	 * available for download and deletion.
	 *
	 * @return The web resources of a type available for download and deletion.
	 * @throws IOException If some I/O error occurs during the generation of the
	 *                     resource list.
	 */
	private HTTPResponse resourceListResponse() throws IOException {
		final String listHtml = request.getServer().getStaticResourceReader().readTextResourceToString(
			"/es/uvigo/esei/dai/hybridserver/resources/res_list.htm"
		);

		if (listHtml == null) {
			throw new IOException(
				"Tried to generate a web resource list, but the needed resource template couldn't be loaded"
			);
		}

		final HybridServer server = request.getServer();

		final HTTPResponse response = new HTTPResponse()
			.setStatus(HTTPResponseStatus.S200)
			.setVersion(HTTPHeaders.HTTP_1_1.getHeader())
			.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
			.putParameter(HTTPHeaders.CONTENT_LANGUAGE.getHeader(), "en");

		final List<ServerConfiguration> remoteServers = server.getConfiguration().getServers();
		final int numberOfRemoteServers = remoteServers.size();
		final Set<UUID> localWebResources = server.getWebResourceDAO(getWebResourceType()).localUuidSet();

		final StringBuilder localResourceListBuilder = new StringBuilder(NO_RES_AVAILABLE.length());
		final Set<String> partialResourceLists = new ConcurrentSkipListSet<>();
		final CountDownLatch resourceListBuilderLatch = new CountDownLatch(numberOfRemoteServers);

		// Generate the HTML code of the list, to substitute on the template

		// First, add the local server resources
		if (!localWebResources.isEmpty()) {
			localResourceListBuilder.append("<h2>This server</h2>\n");

			localResourceListBuilder.append("<ul>\n");
			for (final UUID webResourceUuid : localWebResources) {
				appendWebResourceToContent(localResourceListBuilder, webResourceUuid, null);
			}
			localResourceListBuilder.append("</ul>\n");
		}

		// Now go ahead with the resources of remote servers. Ask them in parallel, and merge the results
		// in a serial order, for minimum latency
		for (final ServerConfiguration remoteServer : remoteServers) {
			final Runnable command = () -> {
				// Retrieve the set of remote UUID
				final Set<UUID> remoteServerUuids = RemoteServerWebServiceCommunicationController.get().executeRemoteOperation(
					(final HybridServerWebService hsws) -> {
						try {
							return HybridServerWebService.localUuidSet(hsws, getWebResourceType());
						} catch (final IOException exc) {
							return Collections.emptySet();
						}
					}, remoteServer, server.getLogger()
				);

				// Generate the string fragment that should be appended to the end result
				final StringBuilder remoteResourceListBuilder = new StringBuilder(NO_RES_AVAILABLE.length());

				remoteResourceListBuilder.append("<h2>");
				remoteResourceListBuilder.append(remoteServer.getName());
				remoteResourceListBuilder.append("</h2>\n");

				if (remoteServerUuids == null || remoteServerUuids.isEmpty()) {
					remoteResourceListBuilder.append(NO_RES_AVAILABLE).append('\n');
				} else {
					remoteResourceListBuilder.append("<ul>\n");
					for (final UUID remoteResourceUuid : remoteServerUuids) {
						try {
							appendWebResourceToContent(remoteResourceListBuilder, remoteResourceUuid, remoteServer);
						} catch (final IOException exc) {
							// StringBuilder shouldn't throw this exception
							throw new AssertionError(exc);
						}
					}
					remoteResourceListBuilder.append("</ul>\n");
				}

				// Add the fragment to the fragment set
				partialResourceLists.add(remoteResourceListBuilder.toString());

				resourceListBuilderLatch.countDown();
			};

			try {
				server.getExecutorService().execute(command);
			} catch (final RejectedExecutionException exc) {
				// Server shutting down. Proceed in this thread as a fallback
				command.run();
			}
		}

		// Wait for all servers to return their results
		try {
			resourceListBuilderLatch.await();
		} catch (final InterruptedException ignored) {}

		// Now generate the definitive resource list. The local server goes first,
		// the rest are sorted alphabetically
		final StringBuilder finalResourceListBuilder = new StringBuilder(localResourceListBuilder.length() * (numberOfRemoteServers + 1));

		finalResourceListBuilder.append(localResourceListBuilder);
		final Iterator<String> iter = partialResourceLists.iterator();
		while (iter.hasNext()) {
			finalResourceListBuilder.append(iter.next());
			iter.remove(); // Make GC job easier
		}

		// Add placeholder text if needed
		if (finalResourceListBuilder.length() < 1) {
			finalResourceListBuilder.append(NO_RES_AVAILABLE);
		}

		return response.setContent(listHtml
			.replace("-- RESOURCE LIST PLACEHOLDER --", finalResourceListBuilder.toString())
		);
	}

	/**
	 * Appends a web resource link, in the form of a HTML anchor element, to a
	 * response content builder, which is an appendable sink of characters.
	 *
	 * @param contentBuilder The character sink to append the anchor element to.
	 * @param uuid           The UUID of the web resource to link to.
	 * @param originServer   The server which has the resource with the given UUID.
	 * @throws IOException If an I/O error occurs while appending characters to
	 *                     {@code contentBuilder}.
	 */
	private void appendWebResourceToContent(final Appendable contentBuilder, final UUID uuid, final ServerConfiguration originServer) throws IOException {
		contentBuilder.append("\t<li><a href=\"");

		if (originServer != null) {
			final String remoteHttpAddress = originServer.getHttpAddress();

			contentBuilder.append(remoteHttpAddress);
			contentBuilder.append(remoteHttpAddress.endsWith("/") ? "" : "/");
		}

		contentBuilder.append(getHandledResourceName())
			.append("?uuid=")
			.append(uuid.toString())
			.append("\">")
			.append(uuid.toString())
			.append("</a></li>\n");
	};
}
