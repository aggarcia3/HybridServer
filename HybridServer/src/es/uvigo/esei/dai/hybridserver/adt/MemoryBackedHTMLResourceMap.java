package es.uvigo.esei.dai.hybridserver.adt;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Models a memory backed HTML resource manager, which mantains a map of HTML
 * resources the server has available for clients, identified by their UUID, in
 * RAM. By design, this implementation of {@code IOBackedMap} never throws an
 * {@code IOException}.
 *
 * @author Alejandro González García
 */
public final class MemoryBackedHTMLResourceMap implements IOBackedMap<String, String> {
	private final Map<String, String> htmlResources = new ConcurrentHashMap<>();

	/**
	 * Creates a new HTML resource manager from a map of initial HTML resources.
	 * 
	 * @param initialResources The initial HTML resources.
	 * @throws IllegalArgumentException If some key or value in the map can't be
	 *                                  inserted in the map.
	 * @see MemoryBackedHTMLResourceMap#put
	 */
	public MemoryBackedHTMLResourceMap(final Map<String, String> initialResources) throws IOException {
		putAll(initialResources);
	}

	/**
	 * Creates a new HTML resource manager without initial HTML resources.
	 */
	public MemoryBackedHTMLResourceMap() {}

	@Override
	public int size() throws IOException {
		return htmlResources.size();
	}

	@Override
	public boolean isEmpty() throws IOException {
		return htmlResources.isEmpty();
	}

	@Override
	public boolean containsKey(String key) throws IOException {
		return key != null && htmlResources.containsKey(key.toLowerCase());
	}

	@Override
	public boolean containsValue(String value) throws IOException {
		return value != null && htmlResources.containsValue(value);
	}

	@Override
	public String get(String key) throws IOException {
		return key != null ? htmlResources.get(key.toLowerCase()) : null;
	}

	/**
	 * @throws IllegalArgumentException If the key is not a valid UUID, or the value
	 *                                  is null.
	 */
	@Override
	public String put(String key, String value) throws IOException {
		try {
			// Try to construct an UUID from the key: if that fails,
			// an exception is thrown and the key is not a UUID
			UUID.fromString(key);
			if (value == null) {
				throw new IllegalArgumentException();
			}

			return htmlResources.put(key.toLowerCase(), value);
		} catch (final IllegalArgumentException exc) {
			throw exc;
		}
	}

	@Override
	public String remove(String key) throws IOException {
		return htmlResources.remove(key.toLowerCase());
	}

	/**
	 * @throws IllegalArgumentException If the key is not a valid UUID, or the value
	 *                                  is null.
	 */
	@Override
	public void putAll(Map<? extends String, ? extends String> m) throws IOException {
		for (Map.Entry<? extends String, ? extends String> pair : m.entrySet()) {
			put(pair.getKey(), pair.getValue());
		}
	}

	@Override
	public void clear() throws IOException {
		htmlResources.clear();
	}

	@Override
	public Set<String> keySet() throws IOException {
		return htmlResources.keySet();
	}

	@Override
	public Collection<String> values() throws IOException {
		return htmlResources.values();
	}

	@Override
	public Set<Map.Entry<String, String>> entrySet() throws IOException {
		return htmlResources.entrySet();
	}
}
