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
final class HTTPRequestHandlerController {
	private final static String IO_EXCEPTION_MSG = "An I/O error has occured while handling a request";

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
	 * and output. The invoker should not send any bytes on the streams before or
	 * after calling this method, as this method closes them automatically.
	 */
	public void handleIncoming() {
		final Logger logger = server.getLogger();

		try {
			// Handle the HTTP request from the client to get its
			// response, and send the response
			final HTTPRequestHandler handler = HTTPRequestHandlerFactory.get().handlerForIncoming(server, input);
			logger.log(Level.FINER, "Got a handler for an incoming request. Sending out its response to the socket...");
			handler.handleRequest().print(output);
		} catch (final IOException exc) {
			logger.log(Level.WARNING, IO_EXCEPTION_MSG, exc);
		} finally {
			logger.log(Level.FINER, "Closing connection for incoming request");

			// Close the readers and writers and the streams
			// who are being decorated. We ignore errors because
			// it's not important we failed doing this (when closing
			// the output stream, we might as well close the input one,
			// as they will likely be related to the same socket)
			try {
				output.close();
			} catch (final IOException ignored) {}
			try {
				input.close();
			} catch (final IOException ignored) {}
		}
	}
}
