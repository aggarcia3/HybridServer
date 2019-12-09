package es.uvigo.esei.dai.hybridserver.webresource;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAOConstants.INVALID_RESOURCE;
import static es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAOConstants.WEB_RESOURCE_ALREADY_MAPPED;

/**
 * Models a memory backed web resource DAO, which maintains a {@link Map} of web
 * resources the server has available for clients, identified by their UUID, in
 * RAM. By design, this implementation of {@code WebResourceDataSource} never
 * throws an {@code IOException}. Where applicable, this class behaves like a
 * {@link ConcurrentHashMap}.
 *
 * @param <T> The type of the web resources contained in the DAO.
 *
 * @author Alejandro González García
 */
final class MemoryWebResourceDAO<T extends WebResource<T>> implements WebResourceDAO<T> {
	private final Map<String, T> webResources = new ConcurrentHashMap<>();

	@Override
	public T get(final UUID uuid) throws IOException {
		return uuid == null ? null : webResources.get(uuid.toString());
	}

	@Override
	public void put(final T webResource) throws IOException {
		if (webResource == null) {
			throw new IllegalArgumentException(INVALID_RESOURCE);
		}

		webResources.compute(webResource.getAttribute(WebResource.UUID_ATTRIBUTE), (final String key, final T value) -> {
			if (webResources.containsKey(key)) {
				throw new IllegalStateException(WEB_RESOURCE_ALREADY_MAPPED);
			}

			return webResource;
		});
	}

	@Override
	public boolean remove(final UUID uuid) throws IOException {
		return uuid == null ? false : webResources.remove(uuid.toString()) != null;
	}

	@Override
	public Set<UUID> uuidSet() throws IOException {
		final Set<UUID> toret = new HashSet<>(webResources.size());

		webResources.forEach((final String key, final T value) -> {
			toret.add(UUID.fromString(key));
		});

		return Collections.unmodifiableSet(toret);
	}

	@Override
	public Collection<T> webResources() throws IOException {
		return Collections.unmodifiableCollection(webResources.values());
	}

	/**
	 * @implNote As this implementation doesn't hold any reference to a closable I/O
	 *           resource, closing it is not necessary, and is a no-op.
	 */
	@Override
	public void close() throws IOException {
		// Nothing to close
	}
}
