package es.uvigo.esei.dai.hybridserver;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import static es.uvigo.esei.dai.hybridserver.HybridServer.LOGGER;

/**
 * Contains logic to read resource files of the application. Resource files are
 * bundled with the application and are to be found in the classpath, like
 * libraries.
 *
 * @author Alejandro González García
 */
final class ResourceReader {
	private static final int WRITER_BUFFER_SIZE = 4 * 1024; // 4 KiB
	private static final int TRANSFER_BUFFER_SIZE = 4 * 1024; // 4 KiB

	/**
	 * Reads a text resource to a string.
	 *
	 * @param requestedByClass The class whose classloader should know how to find
	 *                        the resource.
	 * @param name            The name of the resource.
	 * @return The resource, as a string.
	 */
	public static String readTextResourceToString(final Class<?> requestedByClass, final String name) {
		final Object[] logParameters = new Object[] { name, requestedByClass.getSimpleName() };

		LOGGER.log(Level.FINE, "Reading resource {0} for {1}", logParameters);

		// Read the resource from the input stream
		long readStart = System.currentTimeMillis();
		try (final Reader r = new InputStreamReader(requestedByClass.getResourceAsStream(name), StandardCharsets.UTF_8)) {
			// Create a writer backed by a string buffer
			try (final Writer w = new StringWriter(WRITER_BUFFER_SIZE)) {
				final char[] buf = new char[TRANSFER_BUFFER_SIZE];

				// Copy characters from the reader to the writer in chunks
				int charsRead;
				while ((charsRead = r.read(buf)) > 0) {
					w.write(buf, 0, charsRead);
				}

				final String toret = w.toString();
				LOGGER.log(Level.FINE,
						"Done reading resource {0} requested by {1} ({2} ms, {3} characters)",
						new Object[] {
								name, requestedByClass.getSimpleName(),
								System.currentTimeMillis() - readStart, toret.length()
						}
				);

				return toret;
			}
		} catch (final Exception exc) {
			LOGGER.log(Level.SEVERE, "Couldn't get the resource {0} requested by {1}. Using dummy resource instead", logParameters);
			return "This is a placeholder message that indicates that something is wrong with how the server is installed.";
		}
	}
}
