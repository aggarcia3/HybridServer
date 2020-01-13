package es.uvigo.esei.dai.hybridserver.webservices;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.jws.WebService;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.webresources.HTMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.WebResource;
import es.uvigo.esei.dai.hybridserver.webresources.WebResourceDAO;
import es.uvigo.esei.dai.hybridserver.webresources.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSLTWebResource;

/**
 * The service implementation bean of the service implementation interface that
 * defines the operations a Hybrid Server publishes for other Hybrid Servers to
 * use.
 *
 * @author Alejandro González García
 */
@WebService(
	endpointInterface = "es.uvigo.esei.dai.hybridserver.webservices.HybridServerWebService",
	targetNamespace = "http://hybridserver.dai.esei.uvigo.es/",
	serviceName = "HybridServerService",
	portName = "HybridServerWebServicePort"
)
public final class HybridServerWebServiceImplementation implements HybridServerWebService {
	private final HybridServer localServer;

	/**
	 * Creates a new implementation of the web service interface for a local Hybrid
	 * Server.
	 *
	 * @param localServer The local Hybrid Server.
	 * @throws IllegalArgumentException If {@code localServer} is {@code null}.
	 */
	public HybridServerWebServiceImplementation(final HybridServer localServer) {
		if (localServer == null) {
			throw new IllegalArgumentException(
				"Can't create a Hybrid Server web service implementation for a null local Hybrid Server"
			);
		}

		this.localServer = localServer;
	}

	@Override
	public HTMLWebResource localHTMLGet(final UUID uuid) throws IOException {
		return localGet(HTMLWebResource.class, uuid);
	}

	@Override
	public XMLWebResource localXMLGet(final UUID uuid) throws IOException {
		return localGet(XMLWebResource.class, uuid);
	}

	@Override
	public XSDWebResource localXSDGet(final UUID uuid) throws IOException {
		return localGet(XSDWebResource.class, uuid);
	}

	@Override
	public XSLTWebResource localXSLTGet(final UUID uuid) throws IOException {
		return localGet(XSLTWebResource.class, uuid);
	}

	@Override
	public boolean localHTMLRemove(final UUID uuid) throws IOException {
		return localRemove(HTMLWebResource.class, uuid);
	}

	@Override
	public boolean localXMLRemove(final UUID uuid) throws IOException {
		return localRemove(XMLWebResource.class, uuid);
	}

	@Override
	public boolean localXSDRemove(final UUID uuid) throws IOException {
		return localRemove(XSDWebResource.class, uuid);
	}

	@Override
	public boolean localXSLTRemove(final UUID uuid) throws IOException {
		return localRemove(XSLTWebResource.class, uuid);
	}

	@Override
	public Set<UUID> localHTMLUuidSet() throws IOException {
		return localUuidSet(HTMLWebResource.class);
	}

	@Override
	public Set<UUID> localXMLUuidSet() throws IOException {
		return localUuidSet(XMLWebResource.class);
	}

	@Override
	public Set<UUID> localXSDUuidSet() throws IOException {
		return localUuidSet(XSDWebResource.class);
	}

	@Override
	public Set<UUID> localXSLTUuidSet() throws IOException {
		return localUuidSet(XSLTWebResource.class);
	}

	@Override
	public Collection<HTMLWebResource> localHTMLWebResources() throws IOException {
		return localWebResources(HTMLWebResource.class);
	}

	@Override
	public Collection<XMLWebResource> localXMLWebResources() throws IOException {
		return localWebResources(XMLWebResource.class);
	}

	@Override
	public Collection<XSDWebResource> localXSDWebResources() throws IOException {
		return localWebResources(XSDWebResource.class);
	}

	@Override
	public Collection<XSLTWebResource> localXSLTWebResources() throws IOException {
		return localWebResources(XSLTWebResource.class);
	}

	/**
	 * Returns the web resource associated to the specified UUID, or {@code null} if
	 * the server has no web resources with that UUID.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @param uuid            The UUID whose associated web resource is to be
	 *                        returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if the server doesn't have a web resource for the
	 *         specified type with that UUID.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	private <T extends WebResource<T>> T localGet(final Class<T> webResourceType, final UUID uuid) throws IOException {
		final WebResourceDAO<T> webResourceDao = localServer.getWebResourceDAO(webResourceType);

		return webResourceDao == null ? null : webResourceDao.localGet(uuid);
	}

	/**
	 * Removes the web resource for a UUID from the server, if it is present.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @param uuid            UUID whose web resource is to be removed from the
	 *                        server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	private <T extends WebResource<T>> boolean localRemove(final Class<T> webResourceType, final UUID uuid) throws IOException {
		final WebResourceDAO<T> webResourceDao = localServer.getWebResourceDAO(webResourceType);

		return webResourceDao == null ? false : webResourceDao.localRemove(uuid);
	}

	/**
	 * Returns the UUID of the web resources that the server has.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	private <T extends WebResource<T>> Set<UUID> localUuidSet(final Class<T> webResourceType) throws IOException {
		final WebResourceDAO<T> webResourceDao = localServer.getWebResourceDAO(webResourceType);

		return webResourceDao == null ? Collections.emptySet() : webResourceDao.localUuidSet();
	}

	/**
	 * Returns a {@link Collection} of the web resources that the server has.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	private <T extends WebResource<T>> Collection<T> localWebResources(final Class<T> webResourceType) throws IOException {
		final WebResourceDAO<T> webResourceDao = localServer.getWebResourceDAO(webResourceType);

		return webResourceDao == null ? Collections.emptyList() : webResourceDao.localWebResources();
	}
}
