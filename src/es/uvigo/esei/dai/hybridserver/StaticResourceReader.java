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

	private static final Map<String, String> LOADED_RESOURCES = new ConcurrentHashMap<>(6); // 6 is the number of resources
	private final Logger logger;

	/**
	 * Creates a new static resource reader for a server.
	 *
	 * @param logger The logger responsible for logging the static resource reader
	 *               operations. It might be {@code null}.
	 */
	public StaticResourceReader(final Logger logger) {
		this.logger = logger;
	}

	/**
	 * Creates a new static resource reader for a server, without any logger
	 * associated to it.
	 */
	public StaticResourceReader() {
		this(null);
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
		logIfLoggerAvailable(Level.FINE, "Serving resource {0} from the cache", name);

		return LOADED_RESOURCES.computeIfAbsent(name, (final String resource) -> {
			logIfLoggerAvailable(Level.FINE, "Resource not in cache. Reading resource {0} from storage", resource);

			// Read the resource from the input stream
			long readStart = System.currentTimeMillis();
			try (final Reader r = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8)) {
				final StringBuilder sb = new StringBuilder();
				final char[] buf = new char[TRANSFER_BUFFER_SIZE];

				// Copy characters from the reader to the string builder in chunks
				int charsRead;
				while ((charsRead = r.read(buf)) > -1) {
					sb.append(buf, 0, charsRead);
				}

				final String toret = sb.toString();
				logIfLoggerAvailable(Level.FINE,
					"Done reading resource {0} ({1} ms, {2} characters)",
					new Object[] {
						name,
						System.currentTimeMillis() - readStart, toret.length()
					}
				);

				return toret;
			} catch (final Exception exc) {
				logIfLoggerAvailable(Level.SEVERE, "Couldn't get the resource {0}. Returning null string", resource);
				return null;
			}
		});
	}

	/**
	 * Writes a message to the logger associated with this resource reader, if there
	 * is one.
	 *
	 * @param level  The level of the message.
	 * @param msg    The message template to print.
	 * @param params Any parameters for the message template.
	 */
	private final void logIfLoggerAvailable(final Level level, final String msg, final Object... params) {
		if (logger != null) {
			logger.log(level, msg, params);
		}
	}
}
