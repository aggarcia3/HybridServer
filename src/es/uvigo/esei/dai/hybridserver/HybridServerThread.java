package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import es.uvigo.esei.dai.hybridserver.webservices.HybridServerWebServiceImplementation;

/**
 * Implements the main server thread, responsible for binding the server TCP
 * socket to a port, accepting incoming connections and dispatching them to
 * worker threads.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
final class HybridServerThread extends Thread {
	private final HybridServer server;
	private final Object stateLock = new Object();

	private boolean ready = false;
	private boolean notAbleToBindToPort = false;

	/**
	 * Creates a new main server thread for a Hybrid Server.
	 *
	 * @param server The Hybrid Server to create a main server thread for.
	 */
	HybridServerThread(final HybridServer server) {
		if (server == null) {
			throw new IllegalArgumentException(
				"A Hybrid Server thread can't be associated to a null Hybrid Server"
			);
		}

		this.server = server;

		setPriority(MAX_PRIORITY);
		setDaemon(true);
		setName("Hybrid Server main server thread");

		try {
			setUncaughtExceptionHandler((final Thread t, final Throwable e) -> {
				if (!(e instanceof ThreadDeath)) {
					server.getLogger().log(Level.WARNING,
						"An uncontrolled exception has occured in the main Hybrid Server thread. " +
						"The server will attempt to restart itself, but this could be a symptom of " +
						"a problem.", e
					);

					server.stop();
					server.start();
				}
			});
		} catch (final SecurityException exc) {
			// This shouldn't happen, but this failure is not essential for the application
			// to work. The associated operation just improves availability
		}
	}

	/**
	 * Checks whether this thread encountered an error when binding to a listening
	 * port, usually caused by that port being in used by another application.
	 *
	 * @return True if an error occurred while binding to a listening port, false
	 *         otherwise.
	 */
	boolean isPortInUse() {
		synchronized (stateLock) {
			return notAbleToBindToPort;
		}
	}

	/**
	 * Returns whether the main server thread represented by this object is ready to
	 * accept client connections or not.
	 *
	 * @return True if the thread is ready to accept client connections, false in
	 *         other case.
	 */
	boolean isReady() {
		synchronized (stateLock) {
			return ready;
		}
	}

	/**
	 * Returns the object whose monitor is to be used when being notified about any
	 * thread state changes is desired. That is, any change on the return values of
	 * {@link isPortInUse} and {@link isReady} will notify all the threads waiting
	 * for the returned object monitor via the standard {@link Object#notifyAll}
	 * mechanism, so threads interested in waiting for any update should acquire the
	 * returned object monitor and {@link Object#wait}.
	 *
	 * @return The described object, whose monitor is useful for synchronization. It
	 *         is guaranteed that this method always returns the same object for a
	 *         given instance.
	 */
	Object getStateLock() {
		return stateLock;
	}

	@Override
	public void run() {
		final Logger serverLogger = server.getLogger();
		final ExecutorService executorService = server.getExecutorService();

		Endpoint webServiceEndpoint = null;

		serverLogger.log(Level.INFO, "Starting main server thread");

		try {
			final String serviceUrl = server.getConfiguration().getWebServiceURL();

			if (serviceUrl != null) {
				try {
					// The Javadoc about what exceptions this method throws is weak.
					// Experimentation shows that it throws subclasses of WebServiceException
					// too, to indicate network failures and other kind of JAX-WS related
					// exceptions (possibly the writers of the Javadoc assumed that programmers
					// would realize that, and it arguably makes sense)
					webServiceEndpoint = Endpoint.publish(
						serviceUrl,
						new HybridServerWebServiceImplementation(server)
					);
					webServiceEndpoint.setExecutor(executorService);

					serverLogger.log(Level.INFO, "Web service endpoint published on {0}", serviceUrl);
				} catch (final SecurityException | IllegalArgumentException | IllegalStateException | WebServiceException exc) {
					serverLogger.log(
						Level.SEVERE,
						"An exception has occurred while publishing the web service endpoint. The server won't start",
						exc
					);

					synchronized (stateLock) {
						notAbleToBindToPort = true;
						stateLock.notifyAll();
					}
				}
			}

			if (serviceUrl == null || webServiceEndpoint != null) {
				try (
					final ServerSocketChannel serverSocket = ServerSocketChannel.open().bind(
						new InetSocketAddress(server.getConfiguration().getHttpPort())
					)
				) {
					serverLogger.log(Level.INFO, "Listening on {0} for incoming connections", serverSocket.getLocalAddress());

					// Tell other threads we're ready to accept connections
					synchronized (stateLock) {
						ready = true;
						stateLock.notifyAll();
					}

					// Keep accepting incoming connections until other thread signals us to stop
					while (!interrupted()) {
						try {
							// Hand off the incoming connection to a worker thread
							final SocketChannel socketChannel = serverSocket.accept();
							executorService.execute(() -> {
								final Socket socket = socketChannel.socket();

								try {
									serverLogger.log(Level.FINE, "Received connection from {0}", socketChannel.getRemoteAddress());

									// Handle the request
									try (
										final HTTPRequestHandlerController requestHandlerController = new HTTPRequestHandlerController(
											server,
											socket.getInputStream(),
											socket.getOutputStream()
										)
									) {
										requestHandlerController.handleIncoming();
									}
								} catch (final IOException exc) {
									serverLogger.log(Level.WARNING, HTTPRequestHandlerController.IO_EXCEPTION_MSG, exc);
								}
							});
						} catch (final IOException exc) {
							// Report I/O exceptions, but do not report signals received by other
							// threads to stop
							if (!(exc instanceof ClosedByInterruptException)) {
								serverLogger.log(Level.WARNING, "An I/O error occured while processing a response to a client", exc);
							}
						}
					}
				} catch (final IOException exc) {
					serverLogger.log(Level.SEVERE, "Couldn't bind to port {0}", server.getConfiguration());

					synchronized (stateLock) {
						notAbleToBindToPort = true;
						stateLock.notifyAll();
					}
				}
			}
		} finally {
			serverLogger.log(Level.INFO, "Stopping main server thread");

			// Stop the published web service endpoint
			if (webServiceEndpoint != null) {
				try {
					webServiceEndpoint.stop();

					serverLogger.log(
						Level.INFO,
						"Web service endpoint stopped. Incoming connections will be disregarded"
					);
				} catch (final WebServiceException exc) {
					// Again, the Javadoc for the stop method is weak, but it
					// makes sense that it throws this exception.
					// Maybe this kind of not-so-edge-cases-but-ignored-by-convenience
					// unchecked exceptions should be checked during development,
					// so people doesn't forget about them in the docs :)
					serverLogger.log(
						Level.WARNING,
						"An exception has occurred while trying to stop the web service endpoint",
						exc
					);
				}
			}

			// Stop accepting new tasks for the threads
			executorService.shutdown();

			boolean serviceThreadsTerminated = false;
			try {
				// Wait for service threads to stop
				serviceThreadsTerminated = executorService.awaitTermination(
					server.getStopWaitSeconds(),
					TimeUnit.SECONDS
				);
			} catch (final InterruptedException ignored) {
				// Ignore, we do the appropriate thing in the finally clause
			} finally {
				if (serviceThreadsTerminated) {
					synchronized (stateLock) {
						ready = false;
						stateLock.notifyAll();
					}
				}
			}
		}
	}
}
