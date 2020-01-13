package es.uvigo.esei.dai.hybridserver.webservices;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import javax.jws.WebMethod;
import javax.jws.WebService;

import es.uvigo.esei.dai.hybridserver.webresources.HTMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.WebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSLTWebResource;

/**
 * The service implementation interface that defines the operations a Hybrid
 * Server publishes for other Hybrid Servers.
 *
 * @author Alejandro González García
 */
@WebService(
	targetNamespace = "http://hybridserver.dai.esei.uvigo.es/",
	serviceName = "HybridServerService",
	portName = "HybridServerWebServicePort"
)
// Generics can't be used here, because they don't retain the concrete type information at runtime
// for JAX-WS marshalling and unmarshalling, and that would generate heap pollution.
// Admire how much boilerplate code that design decision can generate :D
public interface HybridServerWebService {
	/**
	 * Returns the HTML web resource associated to the specified UUID, or
	 * {@code null} if the server has no web resources with that UUID. There can be
	 * at most one web resource for a given UUID.
	 *
	 * @param uuid            The UUID whose associated web resource is to be
	 *                        returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if the server doesn't have a web resource for the
	 *         specified type with that UUID.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public HTMLWebResource localHTMLGet(final UUID uuid) throws IOException;

	/**
	 * Returns the XML web resource associated to the specified UUID, or
	 * {@code null} if the server has no web resources with that UUID. There can be
	 * at most one web resource for a given UUID.
	 *
	 * @param uuid            The UUID whose associated web resource is to be
	 *                        returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if the server doesn't have a web resource for the
	 *         specified type with that UUID.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public XMLWebResource localXMLGet(final UUID uuid) throws IOException;

	/**
	 * Returns the XSD web resource associated to the specified UUID, or
	 * {@code null} if the server has no web resources with that UUID. There can be
	 * at most one web resource for a given UUID.
	 *
	 * @param uuid            The UUID whose associated web resource is to be
	 *                        returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if the server doesn't have a web resource for the
	 *         specified type with that UUID.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public XSDWebResource localXSDGet(final UUID uuid) throws IOException;

	/**
	 * Returns the XSLT web resource associated to the specified UUID, or
	 * {@code null} if the server has no web resources with that UUID. There can be
	 * at most one web resource for a given UUID.
	 *
	 * @param uuid            The UUID whose associated web resource is to be
	 *                        returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if the server doesn't have a web resource for the
	 *         specified type with that UUID.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public XSLTWebResource localXSLTGet(final UUID uuid) throws IOException;

	/**
	 * Removes the HTML web resource for a UUID from the server, if it is present.
	 *
	 * @param uuid UUID whose web resource is to be removed from the server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public boolean localHTMLRemove(final UUID uuid) throws IOException;

	/**
	 * Removes the XML web resource for a UUID from the server, if it is present.
	 *
	 * @param uuid UUID whose web resource is to be removed from the server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public boolean localXMLRemove(final UUID uuid) throws IOException;

	/**
	 * Removes the XSD web resource for a UUID from the server, if it is present.
	 *
	 * @param uuid UUID whose web resource is to be removed from the server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public boolean localXSDRemove(final UUID uuid) throws IOException;

	/**
	 * Removes the XSLT web resource for a UUID from the server, if it is present.
	 *
	 * @param uuid UUID whose web resource is to be removed from the server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public boolean localXSLTRemove(final UUID uuid) throws IOException;

	/**
	 * Returns the UUID of the HTML web resources that the server has.
	 *
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Set<UUID> localHTMLUuidSet() throws IOException;

	/**
	 * Returns the UUID of the XML web resources that the server has.
	 *
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Set<UUID> localXMLUuidSet() throws IOException;

	/**
	 * Returns the UUID of the XSD web resources that the server has.
	 *
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Set<UUID> localXSDUuidSet() throws IOException;

	/**
	 * Returns the UUID of the XSLT web resources that the server has.
	 *
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Set<UUID> localXSLTUuidSet() throws IOException;

	/**
	 * Returns a {@link Collection} of the HTML web resources that the server has.
	 *
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Collection<HTMLWebResource> localHTMLWebResources() throws IOException;

	/**
	 * Returns a {@link Collection} of the XML web resources that the server has.
	 *
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Collection<XMLWebResource> localXMLWebResources() throws IOException;

	/**
	 * Returns a {@link Collection} of the XSD web resources that the server has.
	 *
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Collection<XSDWebResource> localXSDWebResources() throws IOException;

	/**
	 * Returns a {@link Collection} of the XSLT web resources that the server has.
	 *
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	public Collection<XSLTWebResource> localXSLTWebResources() throws IOException;

	/**
	 * Provides a bridge between the non-generic get operations defined by this
	 * interface, due to JAX-WS restrictions, and a generic version of them. Users
	 * are encouraged to call this method instead its alternatives wherever
	 * possible.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param hsws            An implementation of this interface, where the
	 *                        operations will be executed. It is assumed that it is
	 *                        not {@code null}.
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
	@WebMethod(exclude = true)
	public static <T extends WebResource<T>> T localGet(
		final HybridServerWebService hsws, final Class<T> webResourceType, final UUID uuid
	) throws IOException {
		if (webResourceType == HTMLWebResource.class) {
			return webResourceType.cast(hsws.localHTMLGet(uuid));
		} else if (webResourceType == XMLWebResource.class) {
			return webResourceType.cast(hsws.localXMLGet(uuid));
		} else if (webResourceType == XSDWebResource.class) {
			return webResourceType.cast(hsws.localXSDGet(uuid));
		} else if (webResourceType == XSLTWebResource.class) {
			return webResourceType.cast(hsws.localXSLTGet(uuid));
		} else {
			throw new AssertionError(
				"Unexpected web resource type: " + webResourceType + ". Has the application been modified recently?"
			);
		}
	}

	/**
	 * Provides a bridge between the non-generic remove operations defined by this
	 * interface, due to JAX-WS restrictions, and a generic version of them. Users
	 * are encouraged to call this method instead its alternatives wherever
	 * possible.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param hsws            An implementation of this interface, where the
	 *                        operations will be executed. It is assumed that it is
	 *                        not {@code null}.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @param uuid            UUID whose web resource is to be removed from the
	 *                        server.
	 * @return True if the associated web resource was found and deleted from the
	 *         server, false otherwise.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	@WebMethod(exclude = true)
	public static <T extends WebResource<T>> boolean localRemove(
		final HybridServerWebService hsws, final Class<T> webResourceType, final UUID uuid
	) throws IOException {
		if (webResourceType == HTMLWebResource.class) {
			return hsws.localHTMLRemove(uuid);
		} else if (webResourceType == XMLWebResource.class) {
			return hsws.localXMLRemove(uuid);
		} else if (webResourceType == XSDWebResource.class) {
			return hsws.localXSDRemove(uuid);
		} else if (webResourceType == XSLTWebResource.class) {
			return hsws.localXSLTRemove(uuid);
		} else {
			throw new AssertionError(
				"Unexpected web resource type: " + webResourceType + ". Has the application been modified recently?"
			);
		}
	}

	/**
	 * Provides a bridge between the non-generic UUID set operations defined by this
	 * interface, due to JAX-WS restrictions, and a generic version of them. Users
	 * are encouraged to call this method instead its alternatives wherever
	 * possible.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param hsws            An implementation of this interface, where the
	 *                        operations will be executed. It is assumed that it is
	 *                        not {@code null}.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @return A set containing the UUID of the web resources with that type. This
	 *         set can be an unmodifiable set.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	@WebMethod(exclude = true)
	public static <T extends WebResource<T>> Set<UUID> localUuidSet(
		final HybridServerWebService hsws, final Class<T> webResourceType
	) throws IOException {
		if (webResourceType == HTMLWebResource.class) {
			return hsws.localHTMLUuidSet();
		} else if (webResourceType == XMLWebResource.class) {
			return hsws.localXMLUuidSet();
		} else if (webResourceType == XSDWebResource.class) {
			return hsws.localXSDUuidSet();
		} else if (webResourceType == XSLTWebResource.class) {
			return hsws.localXSLTUuidSet();
		} else {
			throw new AssertionError(
				"Unexpected web resource type: " + webResourceType + ". Has the application been modified recently?"
			);
		}
	}

	/**
	 * Provides a bridge between the non-generic web resources retrieval operations
	 * defined by this interface, due to JAX-WS restrictions, and a generic version
	 * of them. Users are encouraged to call this method instead its alternatives
	 * wherever possible.
	 *
	 * @param <T>             The type of web resource to operate with.
	 * @param hsws            An implementation of this interface, where the
	 *                        operations will be executed. It is assumed that it is
	 *                        not {@code null}.
	 * @param webResourceType The class that represents the type of web resource to
	 *                        operate with.
	 * @return The collection of the web resources that the server has.
	 * @throws IOException If some I/O error occurred in the remote server during
	 *                     the operation.
	 */
	@WebMethod(exclude = true)
	@SuppressWarnings("unchecked") // Casts safe by design
	public static <T extends WebResource<T>> Collection<T> localWebResources(
		final HybridServerWebService hsws, final Class<T> webResourceType
	) throws IOException {
		if (webResourceType == HTMLWebResource.class) {
			return (Collection<T>) hsws.localHTMLWebResources();
		} else if (webResourceType == XMLWebResource.class) {
			return (Collection<T>) hsws.localXMLWebResources();
		} else if (webResourceType == XSDWebResource.class) {
			return (Collection<T>) hsws.localXSDWebResources();
		} else if (webResourceType == XSLTWebResource.class) {
			return (Collection<T>) hsws.localXSLTWebResources();
		} else {
			throw new AssertionError(
				"Unexpected web resource type: " + webResourceType + ". Has the application been modified recently?"
			);
		}
	}
}
