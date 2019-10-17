package es.uvigo.esei.dai.hybridserver;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains logic to read resource files of the application. Resource files are
 * bundled with the application and are to be found in the classpath, like
 * libraries.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class ResourceReader {
	private static final int WRITER_BUFFER_SIZE = 4 * 1024; // 4 KiB
	private static final int TRANSFER_BUFFER_SIZE = 4 * 1024; // 4 KiB

	private final Map<String,String> loadedTextResources = new ConcurrentHashMap<>(3); // 3 is the number of resources
	private final HybridServer server;

	/**
	 * Creates a new resource reader for a server.
	 *
	 * @param server The server whose class loader is responsible for the resource
	 *               loading.
	 */
	public ResourceReader(final HybridServer server) {
		this.server = server;
	}

	/**
	 * Reads a text resource bundled with the server to a string.
	 *
	 * @param name The name of the resource.
	 * @return The resource, as a string. It can be null if an error occurred during
	 *         the operation; if this is the case, users are not encouraged to retry
	 *         the operation until it returns a different value, because it won't.
	 */
	public String readTextResourceToString(final String name) {
		final Logger logger = server.getLogger();

		logger.log(Level.FINE, "Trying to serve resource {0} from the cache", name);

		return loadedTextResources.computeIfAbsent(name, (String resource) -> {
			logger.log(Level.FINE, "Resource not in cache. Reading resource {0} from storage", name);

			// Read the resource from the input stream
			long readStart = System.currentTimeMillis();
			try (final Reader r = new InputStreamReader(server.getClass().getResourceAsStream(name), StandardCharsets.UTF_8)) {
				// Create a writer backed by a string buffer
				try (final Writer w = new StringWriter(WRITER_BUFFER_SIZE)) {
					final char[] buf = new char[TRANSFER_BUFFER_SIZE];

					// Copy characters from the reader to the writer in chunks
					int charsRead;
					while ((charsRead = r.read(buf)) > 0) {
						w.write(buf, 0, charsRead);
					}

					final String toret = w.toString();
					logger.log(Level.FINE,
						"Done reading resource {0} ({1} ms, {2} characters)",
						new Object[] {
							name,
							System.currentTimeMillis() - readStart, toret.length()
						}
					);

					return toret;
				}
			} catch (final Exception exc) {
				logger.log(Level.SEVERE, "Couldn't get the resource {0}. Returning null string", name);
				return null;
			}
		});
	}
}
