package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPMissingHeaderException;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponse;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.HTTPUnsupportedContentEncodingException;
import es.uvigo.esei.dai.hybridserver.http.HTTPUnsupportedHeaderException;

import static es.uvigo.esei.dai.hybridserver.HybridServer.LOGGER;

/**
 * Orchestrates operations to implement the HTTP request handling business
 * logic.
 *
 * @author Alejandro González García
 */
final class HTTPRequestHandlerController {
	private final static String STATUS_HTML = ResourceReader.get().readTextResourceToString(HTTPRequestHandlerController.class, "/es/uvigo/esei/dai/hybridserver/resources/status_code.htm");
	private final static String IO_EXCEPTION_MSG = "An I/O error has occured while handling a request";

	private final Reader input;
	private final Writer output;
	private final HTTPHeaders version;
	private boolean handledRequest = false;

	/**
	 * Instantiates a new request handler controller.
	 *
	 * @param input  The input stream to associate this controller with.
	 * @param output The output stream to associate this controller with.
	 * @param version The version of the HTTP response to send.
	 */
	public HTTPRequestHandlerController(final InputStream input, final OutputStream output, final HTTPHeaders version) {
		if (input == null || output == null || version == null) {
			throw new IllegalArgumentException("A request handler must be constructed with non-null streams and HTTP version");
		}

		this.input = new InputStreamReader(input, StandardCharsets.UTF_8);
		this.output = new OutputStreamWriter(output, StandardCharsets.UTF_8);
		this.version = version;
	}

	/**
	 * Instantiates a new request handler controller.
	 *
	 * @param input  The input stream to associate this controller with.
	 * @param output The output stream to associate this controller with.
	 */
	public HTTPRequestHandlerController(final InputStream input, final OutputStream output) {
		this(input, output, HTTPHeaders.HTTP_1_1);
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
		if (!handledRequest) {
			try {
				LOGGER.log(Level.FINE, "Handling incoming request");
				final long handlingStart = System.currentTimeMillis();

				// Read a syntactically-valid HTTP request from the client, handle it to get its
				// response, and send the response
				final HTTPRequest request = new HTTPRequest(input);
				final HTTPRequestHandler handler = HTTPRequestHandlerFactory.handlerFor(request);
				final HTTPResponse response = handler.handle();

				LOGGER.log(Level.FINE,
					"Generated response {0} for incoming {1} request to {2} in {3} ms, using {4}",
					new Object[] {
						response.getStatus(),
						request.getMethod(),
						request.getResourceChain(),
						System.currentTimeMillis() - handlingStart,
						handler
					}
				);

				response.print(output);

				// Close the readers and writers and the streams
				// who are being decorated. We ignore errors because
				// it's not important we failed doing this (when closing
				// the output stream, we might as well close the input one,
				// as they will likely be related to the same socket)
				try {
					output.close();
					input.close();
				} catch (final IOException ignore) {}
			} catch (final HTTPParseException exc) {
				final Throwable cause = exc.getCause();

				// Initially, for all other causes of the parsing exception, blame the client
				// with a 400 Bad Request status
				HTTPResponseStatus status = HTTPResponseStatus.S400;

				if (cause instanceof HTTPUnsupportedHeaderException) {
					// Not implemented header, so we should respond with the 501 status code
					status = HTTPResponseStatus.S501;
				} else if (cause instanceof HTTPUnsupportedContentEncodingException) {
					// We should respond with 415 Unsupported Media Type
					status = HTTPResponseStatus.S415;
				} else if (cause instanceof HTTPMissingHeaderException &&
						((HTTPMissingHeaderException) cause).getHeader() == HTTPHeaders.CONTENT_LENGTH
				) {
					status = HTTPResponseStatus.S411;
				} else {
					LOGGER.log(Level.INFO, "The client has sent a bad request", exc);
				}

				try {
					respondStatus(status);
				} catch (final IOException exc2) {
					LOGGER.log(Level.WARNING, IO_EXCEPTION_MSG, exc);
				}
			} catch (final IOException exc) {
				LOGGER.log(Level.WARNING, IO_EXCEPTION_MSG, exc);
			} finally {
				// Close the reader and writer.
				// This way ensures the writer actually writes to the stream
				try {
					input.close();
					output.close();
				} catch (final IOException ignored) {}

				handledRequest = true;
			}
		} else {
			LOGGER.log(Level.FINE, "Request already handled by this controller, ignoring");
		}
	}

	/**
	 * Sends a response with a {@code text/html} body that details its status code.
	 *
	 * @param status The status code of the response.
	 * @throws IOException If some I/O error occurs during the operation.
	 */
	private void respondStatus(final HTTPResponseStatus status) throws IOException {
		LOGGER.log(Level.FINE, "Responding to the client with {0}", status.getCode());

		final HTTPResponse response = new HTTPResponse()
			.setStatus(status)
			.setVersion(version.getHeader());

		if (STATUS_HTML != null) {
			response.putParameter(HTTPHeaders.CONTENT_TYPE.getHeader(), "text/html; charset=UTF-8")
				.setContent(String.format(STATUS_HTML, status.getCode(), status.getStatus()));
		} else {
			LOGGER.log(Level.WARNING, "Couldn't get the HTML status page resource, so no message body was sent for the error");
		}

		response.print(output);
	}
}
