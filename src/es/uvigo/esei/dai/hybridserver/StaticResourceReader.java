package es.uvigo.esei.dai.hybridserver;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains logic to read static resource files of the application. Resource
 * files are bundled with the application and are to be found in the classpath,
 * like libraries.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class StaticResourceReader {
	private static final int TRANSFER_BUFFER_SIZE = 4 * 1024; // 4 KiB

	private final Map<String, String> loadedTextResources = new ConcurrentHashMap<>(5); // 5 is the number of resources
	private final Logger logger;

	/**
	 * Creates a new static resource reader for a server.
	 *
	 * @param logger The logger responsible for logging the static resource reader
	 *               operations.
	 */
	public StaticResourceReader(final Logger logger) {
		this.logger = logger;
	}

	/**
	 * Reads a text resource bundled with the server to a string.
	 *
	 * @param name The name of the resource.
	 * @return The resource, as a string. It can be null if an error occurred during
	 *         the operation; if this is the case, users are not encouraged to retry
	 *         the operation until it returns a different value, because it very likely
	 *         won't.
	 */
	public String readTextResourceToString(final String name) {
		logger.log(Level.FINE, "Serving resource {0} from the cache", name);

		return loadedTextResources.computeIfAbsent(name, (String resource) -> {
			logger.log(Level.FINE, "Resource not in cache. Reading resource {0} from storage", name);

			// Read the resource from the input stream
			long readStart = System.currentTimeMillis();
			try (final Reader r = new InputStreamReader(HybridServer.class.getResourceAsStream(name), StandardCharsets.UTF_8)) {
				final StringBuilder sb = new StringBuilder();
				final char[] buf = new char[TRANSFER_BUFFER_SIZE];

				// Copy characters from the reader to the string builder in chunks
				int charsRead;
				while ((charsRead = r.read(buf)) > -1) {
					sb.append(buf, 0, charsRead);
				}

				final String toret = sb.toString();
				logger.log(Level.FINE,
					"Done reading resource {0} ({1} ms, {2} characters)",
					new Object[] {
						name,
						System.currentTimeMillis() - readStart, toret.length()
					}
				);

				return toret;
			} catch (final Exception exc) {
				logger.log(Level.SEVERE, "Couldn't get the resource {0}. Returning null string", name);
				return null;
			}
		});
	}
}
