package es.uvigo.esei.dai.hybridserver.webresources;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * A interface that provides access to a map-like view of web resources,
 * identified by their UUID, and can be backed by an I/O abstraction, throwing
 * I/O exceptions in data access methods.
 *
 * @param <T> The type of the web resources the DAO gives access to.
 */
public interface WebResourceDAO<T extends WebResource<T>> extends Closeable {
	/**
	 * Returns the web resource associated to the specified UUID, or {@code null} if
	 * this web resource DAO contains no web resource with that UUID. There can be
	 * at most one web resource for a given UUID.
	 *
	 * <p>
	 * Web resource DAOs always contain non-null web resources mapped to non-null
	 * UUIDs, so {@code get(null)} always returns {@code null}.
	 *
	 * @param uuid The UUID whose associated web resource is to be returned.
	 * @return The web resource to which the specified UUID is mapped, or
	 *         {@code null} if this web resource DAO contains no mapping for the
	 *         UUID.
	 * @throws IOException If some I/O error occurred during the operation.
	 */
	public T get(final UUID uuid) throws IOException;

	/**
	 * Returns the locally accessible web resource associated to the specified UUID,
	 * with the same semantics as {@link WebResourceDAO#get}. A web resource is
	 * considered locally accessible if it is found in locally accessible I/O
	 * abstractions (for the purposes of this class, database connections are
	 * considered local). Therefore, this method should not establish network
	 * connections explicitly, or perform any other network-related operation to try
	 * to get the specified web resource.
	 *
	 * @param uuid The UUID whose associated web resource is to be returned.
	 * @return The locally accessible web resource to which the specified UUID is
	 *         mapped.
	 * @throws IOException If some I/O error occurred during the operation.
	 * @implNote The default implementation assumes that the DAO only uses locally
	 *           accessible I/O abstractions, and just invokes
	 *           {@link WebResourceDAO#get}.
	 */
	public default T localGet(final UUID uuid) throws IOException {
		return get(uuid);
	}

	/**
	 * Persistently associates the specified web resource with the specified UUID in
	 * this web resource DAO. If the web resource DAO contained a mapping for the
	 * UUID, the previous web resource DAO is not replaced, and an exception is
	 * thrown (see the Throws section).
	 *
	 * @param webResource The web resource to persist in the DAO.
	 * @throws IllegalStateException    If the web resource DAO already contains a
	 *                                  web resource with the specified UUID.
	 * @throws IllegalArgumentException If the web resource is null.
	 * @throws IOException              If some I/O error occurred during the
	 *                                  operation.
	 */
	public void put(final T webResource) throws IOException;

	/**
	 * Removes the web resource for a UUID from this web resource DAO if it is
	 * present, so that the association will be deleted persistently.
	 *
	 * @param uuid UUID whose web resource is to be removed from the web resource
	 *             DAO.
	 * @return True if the associated web resource was found and deleted from the
	 *         DAO, false otherwise.
	 * @throws IOException If some I/O error occurred during the operation.
	 */
	public boolean remove(final UUID uuid) throws IOException;

	/**
	 * Removes the locally accessible web resource with the given UUID from this
	 * DAO.
	 *
	 * @param uuid UUID whose web resource is to be removed from the web resource
	 *             DAO.
	 * @return True if the associated web resource was found locally and deleted
	 *         from the DAO, false otherwise.
	 * @throws IOException If some I/O error occurred during the operation.
	 * @see WebResourceDAO#localGet
	 */
	public default boolean localRemove(final UUID uuid) throws IOException {
		return remove(uuid);
	}

	/**
	 * Returns a {@link Set} of the UUIDs used by web resources in this DAO. The set
	 * is a read-only collection of UUIDs present in the DAO, retrieved using
	 * weakly-consistent semantics.
	 *
	 * @return A set containing the UUIDs used by web resources in this DAO.
	 * @throws IOException If some I/O error occurred during the operation.
	 */
	public Set<UUID> uuidSet() throws IOException;

	/**
	 * Returns a {@link Set} of the UUIDs used by the locally accessible web
	 * resources in this DAO. The set is a read-only collection of UUIDs, retrieved
	 * using weakly-consistent semantics.
	 *
	 * @return A set containing the UUIDs used by the locally accessible web
	 *         resources in this DAO.
	 * @throws IOException If some I/O error occurred during the operation.
	 * @see WebResourceDAO#localGet
	 */
	public default Set<UUID> localUuidSet() throws IOException {
		return uuidSet();
	}

	/**
	 * Returns a {@link Collection} view of the web resources contained by this data
	 * source. The collection is a read-only collection of the web resources
	 * contained in the DAO, retrieved using weakly-consistent semantics.
	 *
	 * @return A collection of the web resources contained in this DAO.
	 * @throws IOException If some I/O error occurred during the operation.
	 */
	public Collection<T> webResources() throws IOException;

	/**
	 * Returns a {@link Collection} view of the locally accessible web resources
	 * contained by this data source. The collection is a read-only collection of
	 * the locally accessible web resources contained in the DAO, retrieved using
	 * weakly-consistent semantics.
	 *
	 * @return A collection of the locally accessible web resources contained in this DAO.
	 * @throws IOException If some I/O error occurred during the operation.
	 * @see WebResourceDAO#localGet
	 */
	public default Collection<T> localWebResources() throws IOException {
		return webResources();
	}
}
