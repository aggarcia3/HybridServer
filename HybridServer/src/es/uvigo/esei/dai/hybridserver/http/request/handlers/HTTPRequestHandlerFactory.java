package es.uvigo.esei.dai.hybridserver.http.request.handlers;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;

import es.uvigo.esei.dai.hybridserver.HybridServer;
import es.uvigo.esei.dai.hybridserver.http.HTTPHeaders;
import es.uvigo.esei.dai.hybridserver.http.HTTPMissingHeaderException;
import es.uvigo.esei.dai.hybridserver.http.HTTPParseException;
import es.uvigo.esei.dai.hybridserver.http.HTTPRequest;
import es.uvigo.esei.dai.hybridserver.http.HTTPResponseStatus;
import es.uvigo.esei.dai.hybridserver.http.HTTPUnsupportedContentEncodingException;
import es.uvigo.esei.dai.hybridserver.http.HTTPUnsupportedHeaderException;

/**
 * Instantiates the appropriate HTTP request handlers for HTTP requests.
 *
 * @author Alejandro González García
 */
public final class HTTPRequestHandlerFactory {
	private static final String UNCONTROLLED_EXCEPTION_ERROR = "Couldn't get a HTTP request handler for a request. This is a signal of an application logic error that should be fixed";

	// Initialization-on-demand holder idiom
	private static final class HTTPRequestHandlerFactoryInstanceHolder {
		static final HTTPRequestHandlerFactory INSTANCE = new HTTPRequestHandlerFactory();
	}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HTTPRequestHandlerFactory get() {
		return HTTPRequestHandlerFactoryInstanceHolder.INSTANCE;
	}

	/**
	 * Creates a HTTP request handler for a yet to be parsed HTTP request, which
	 * should be read from a input.
	 *
	 * @param server The request to create a HTTP request handler for.
	 * @param input  The Reader instance where the server has an incoming HTTP
	 *               request to handle.
	 * @return The appropriate HTTP request handler for the HTTP request, that will
	 *         return a HTTP response for the request.
	 * @throws IOException If a communication error occurred, and upper layers
	 *                     should try to close the connection ASAP. It can also be
	 *                     thrown if no appropriate HTTP request handler could be
	 *                     created for the request, but this signals an
	 *                     implementation error that shouldn't happen under normal
	 *                     conditions, and should be treated by upper layers like a
	 *                     communication error.
	 */
	public HTTPRequestHandler handlerForIncoming(final HybridServer server, final Reader input) throws IOException {
		server.getLogger().log(Level.FINE, "Creating handler for incoming request");

		try {
			return handlerFor(new HTTPRequest(server, input));
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
			}

			try {
				return new HTTPRequestStatusCodeHandler(null, null, status);
			} catch (final Exception exc2) {
				server.getLogger().log(Level.SEVERE, UNCONTROLLED_EXCEPTION_ERROR, exc);
				throw new IOException(exc2);
			}
		} catch (final Exception exc) {
			server.getLogger().log(Level.SEVERE, UNCONTROLLED_EXCEPTION_ERROR, exc);
			throw new IOException(exc);
		}
	}

	/**
	 * Creates a HTTP request handler for a supplied HTTP request.
	 *
	 * @param request The request to create a HTTP request handler for.
	 * @return The appropriate HTTP request handler for the HTTP request, that will
	 *         return a HTTP response for the request.
	 */
	private HTTPRequestHandler handlerFor(final HTTPRequest request) {
		final HTTPRequestHandlerBuilder handlerChainBuilder = new HTTPRequestHandlerBuilder(request);

		// Ordered from more specific (higher priority, first checked)
		// to more general (lower priority, last checked).
		// The order is only relevant if two or more handlers are able
		// to handle the request
		handlerChainBuilder
			.setFirstHandler(HTTPGETRequestHTMLResourceHandler.class)
			.setNextHandler(HTTPRequestWelcomePageHandler.class)
			.setNextHandler(HTTPRequestStatusCodeHandler.class); // 400 Bad Request

		return handlerChainBuilder.build();
	}

	/**
	 * Provides syntactic sugar for creating a chain of responsibility of
	 * {@code HTTPRequestHandler}s, encapsulating the composition logic and allowing
	 * to express it in a fluent way.
	 *
	 * @author Alejandro González García
	 */
	private static final class HTTPRequestHandlerBuilder {
		private final HTTPRequest request;
		private Class<? extends HTTPRequestHandler> concreteHandler;
		private HTTPRequestHandlerBuilder nextHandlerBuilder;

		/**
		 * Creates a new HTTP request handler chain, with all the participating handlers
		 * associated to a {@link HTTPRequest}, and a {@link HTTPRequestHandler}
		 * associated to the current position in the chain.
		 *
		 * @param request         The HTTP request to create a chain of handlers for.
		 * @param concreteHandler The concrete handler associated to the current
		 *                        position in the chain. It may be null, but if that's
		 *                        the case then it should be initialized later by
		 *                        calling
		 *                        {@link HTTPRequestHandlerBuilder#setFirstHandler}.
		 */
		private HTTPRequestHandlerBuilder(final HTTPRequest request, final Class<? extends HTTPRequestHandler> concreteHandler) {
			this.request = request;
			this.concreteHandler = concreteHandler;
		}

		/**
		 * Creates a new HTTP request handler chain, with all the participating handlers
		 * associated to a HTTPRequest, with no HTTP request handler in the current
		 * position of the chain yet. That position should be initialized later by
		 * calling {@link HTTPRequestHandlerBuilder#setFirstHandler}.
		 *
		 * @param request The HTTP request to create a chain of handlers for.
		 */
		public HTTPRequestHandlerBuilder(final HTTPRequest request) {
			this(request, null);
		}

		/**
		 * Associates a concrete HTTP request handler with the current position in the
		 * chain.
		 *
		 * @param firstConcreteHandler The described HTTP request handler.
		 * @return This HTTP request handler builder, to allow for a fluent interface.
		 */
		public HTTPRequestHandlerBuilder setFirstHandler(final Class<? extends HTTPRequestHandler> firstConcreteHandler) {
			this.concreteHandler = firstConcreteHandler;
			return this;
		}

		/**
		 * Associates a concrete HTTP request handler with the next position in the
		 * chain, creating a builder to allow appending more builders to it.
		 *
		 * @param nextConcreteHandler The described HTTP request handler.
		 * @return The created HTTP request handler builder, with the next position in
		 *         the chain being its first one. This allows to append concrete HTTP
		 *         request handlers in a fluent manner, by chaining calls to this
		 *         method.
		 */
		public HTTPRequestHandlerBuilder setNextHandler(final Class<? extends HTTPRequestHandler> nextConcreteHandler) {
			this.nextHandlerBuilder = new HTTPRequestHandlerBuilder(request, nextConcreteHandler);
			return nextHandlerBuilder;
		}

		/**
		 * Builds a chained HTTP request handler, starting from the current (first)
		 * position represented by this builder, and recursively appending the next
		 * builders in the chain.
		 *
		 * @return The chained HTTP request handler.
		 * @throws IllegalStateException If a concrete handler wasn't associated at the
		 *                               current point of the chain.
		 */
		public HTTPRequestHandler build() {
			if (concreteHandler == null) {
				throw new IllegalArgumentException("No concrete HTTP request handler in a position of the HTTP request handler chain");
			}

			try {
				return concreteHandler.getConstructor(HTTPRequest.class, HTTPRequestHandler.class)
					.newInstance(request, nextHandlerBuilder == null ? null : nextHandlerBuilder.build()
				);
			} catch (final ReflectiveOperationException exc) {
				// This shouldn't happen in a well implemented application
				throw new AssertionError(exc);
			}
		}
	}
}
