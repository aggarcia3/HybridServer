package es.uvigo.esei.dai.hybridserver.webresource;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Models a memory backed web resource map, which maintains a collection of web
 * resources the server has available for clients, identified by their UUID, in
 * RAM. By design, this implementation of {@code IOBackedMap} never throws an
 * {@code IOException}. For the most part, this class behaves like a
 * {@link ConcurrentHashMap}.
 *
 * @author Alejandro González García
 */
final class MemoryBackedWebResourceMap implements IOBackedWebResourceMap<String, WebResource> {
	private final Map<String, WebResource> webResources = new ConcurrentHashMap<>();

	@Override
	public int size() throws IOException {
		return webResources.size();
	}

	@Override
	public boolean isEmpty() throws IOException {
		return webResources.isEmpty();
	}

	@Override
	public boolean containsKey(String key) throws IOException {
		return key != null && webResources.containsKey(key.toLowerCase());
	}

	@Override
	public boolean containsValue(WebResource value) throws IOException {
		return value != null && webResources.containsValue(value);
	}

	@Override
	public WebResource get(String key) throws IOException {
		return key != null ? webResources.get(key.toLowerCase()) : null;
	}

	@Override
	public WebResource put(String key, WebResource value) throws IOException {
		try {
			// Try to construct an UUID from the key: if that fails,
			// an exception is thrown and the key is not a UUID
			UUID.fromString(key);
			if (value == null) {
				throw new IllegalArgumentException();
			}

			return webResources.put(key.toLowerCase(), value);
		} catch (final IllegalArgumentException exc) {
			throw exc;
		}
	}

	@Override
	public WebResource remove(String key) throws IOException {
		return webResources.remove(key.toLowerCase());
	}

	@Override
	public void putAll(Map<? extends String, ? extends WebResource> m) throws IOException {
		for (Map.Entry<? extends String, ? extends WebResource> pair : m.entrySet()) {
			put(pair.getKey(), pair.getValue());
		}
	}

	@Override
	public void clear() throws IOException {
		webResources.clear();
	}

	@Override
	public Set<String> keySet() throws IOException {
		return webResources.keySet();
	}

	@Override
	public Collection<WebResource> values() throws IOException {
		return webResources.values();
	}

	@Override
	public Set<Map.Entry<String, WebResource>> entrySet() throws IOException {
		return webResources.entrySet();
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
