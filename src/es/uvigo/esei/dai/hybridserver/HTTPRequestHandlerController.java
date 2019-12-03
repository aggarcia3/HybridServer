package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.http.request.handlers.HTTPRequestHandler;
import es.uvigo.esei.dai.hybridserver.http.request.handlers.HTTPRequestHandlerFactory;

/**
 * Orchestrates operations to implement the HTTP request handling business
 * logic.
 *
 * @author Alejandro González García
 */
final class HTTPRequestHandlerController implements AutoCloseable {
	final static String IO_EXCEPTION_MSG = "An I/O error has occured while handling a request";

	private final HybridServer server;
	private final InputStream input;
	private final Writer output;

	/**
	 * Instantiates a new request handler controller.
	 *
	 * @param server The server for which this controller will handle requests.
	 * @param input  The input stream to associate this controller with.
	 * @param output The output stream to associate this controller with.
	 */
	public HTTPRequestHandlerController(final HybridServer server, final InputStream input, final OutputStream output) {
		if (server == null || input == null || output == null) {
			throw new IllegalArgumentException("A request handler must be constructed with non-null streams and server");
		}

		this.server = server;
		this.input = input;
		this.output = new OutputStreamWriter(output, StandardCharsets.UTF_8);
	}

	/**
	 * Handles an incoming, to be consumed request in the input stream associated to
	 * this controller. When this method returns, it is guaranteed that the server
	 * made its best effort to send the appropriate response to the client. The
	 * caller should ensure that the streams used by this object are ready for input
	 * and output.
	 */
	public void handleIncoming() {
		final Logger logger = server.getLogger();

		try {
			// Handle the HTTP request from the client to get its
			// response, and send the response
			final HTTPRequestHandler handler = HTTPRequestHandlerFactory.get().handlerForIncoming(server, input);
			logger.log(Level.FINER, "Got a handler for an incoming request. Sending out its response to the socket...");
			handler.handleRequest().printTo(output);
		} catch (final IOException exc) {
			logger.log(Level.WARNING, IO_EXCEPTION_MSG, exc);
		} catch (final Throwable exc) {
			logger.log(Level.WARNING,
				"An unexpected exception has occurred while handling an incoming request. No response can be sent to the client", exc
			);

			// "An Error is a subclass of Throwable that indicates serious problems that a
			// reasonable application should not try to catch."
			if (exc instanceof Error) {
				throw exc;
			}
		}
	}

	@Override
	public void close() throws IOException {
		// Wrap each close operation in a try-catch to avoid possible ClosedByInterruptException propagation
		try {
			output.close();
		} catch (final IOException ignored) {}
		try {
			input.close();
		} catch (final IOException ignored) {}
	}
}
