package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.util.IOBackedMap;

public final class HybridServer {
	private static final int STOP_WAIT_MS = 10000;

	private static final int DEFAULT_SERVICE_PORT = 8888;
	private static final short DEFAULT_NUM_CLIENTS = 50;
	private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/hstestdb";
	private static final String DEFAULT_DB_PASSWORD = "hsdbpass";

	// Accesses to this variable should be guarded by serverThreadLock
	private Thread serverThread = null;
	// Accesses to this variable should be guarded by serverReadyLock
	private boolean serverReady = false;
	private final Object serverThreadLock = new Object();
	private final Object serverReadyLock = new Object();

	private final String name = "Hybrid Server";
	private final Logger logger = Logger.getLogger(name);
	private final ResourceReader resourceReader = new ResourceReader(this);
	private final IOBackedMap<String, String> htmlResourceMap;

	private final int servicePort;
	private final short numClients;
	private final String dbUrl;
	private final String dbPassword;

	/**
	 * Creates a Hybrid Server without initial HTML resources, that stores new ones
	 * in memory and will listen on the default service port and is initialized with
	 * default configuration parameters. This constructor is mainly useful for
	 * tests.
	 */
	public HybridServer() {
		this.htmlResourceMap = new MemoryBackedHTMLResourceMap();
		this.servicePort = DEFAULT_SERVICE_PORT;
		this.numClients = DEFAULT_NUM_CLIENTS;
		this.dbUrl = DEFAULT_DB_URL;
		this.dbPassword = DEFAULT_DB_PASSWORD;
	}

	/**
	 * Creates a Hybrid Server whose initial HTML resources are those specified in
	 * the in-memory map. Modifications of HTML resources will not pass-through to
	 * this map. The server will listen on the default service port, and be
	 * initialized with the default configuration parameters. This constructor is
	 * mainly useful for tests.
	 *
	 * @param pages The initial HTML resources of the server. The keys of the map
	 *              are UUIDs, and the values the content associated to that UUID.
	 */
	public HybridServer(final Map<String, String> pages) {
		try {
			this.htmlResourceMap = new MemoryBackedHTMLResourceMap(pages);
		} catch (final IOException exc) {
			// This shouldn't happen
			throw new AssertionError(exc);
		}

		this.servicePort = DEFAULT_SERVICE_PORT;
		this.numClients = DEFAULT_NUM_CLIENTS;
		this.dbUrl = DEFAULT_DB_URL;
		this.dbPassword = DEFAULT_DB_PASSWORD;
	}

	public HybridServer(final Properties properties) {
		// TODO
		this.htmlResourceMap = new MemoryBackedHTMLResourceMap();
		this.servicePort = DEFAULT_SERVICE_PORT;
		this.numClients = DEFAULT_NUM_CLIENTS;
		this.dbUrl = DEFAULT_DB_URL;
		this.dbPassword = DEFAULT_DB_PASSWORD;
	}

	/**
	 * Returns the user-friendly name of this server.
	 *
	 * @return The user-friendly name of this server. Currently, "Hybrid Server".
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the port this server will listen on, if it was not yet started, or is
	 * listening on, if it was started sucessfully.
	 *
	 * @return The port this server will bind a socket on, for clients to connect to.
	 */
	public int getPort() {
		return servicePort;
	}

	/**
	 * Obtains the Java resource reader for use with this server.
	 *
	 * @return The described resource reader.
	 */
	public ResourceReader getResourceReader() {
		return resourceReader;
	}

	/**
	 * Gets the HTML resource map for this Hybrid Server.
	 *
	 * @return The HTML resource map for this server.
	 */
	public IOBackedMap<String, String> getHtmlResourceMap() {
		return htmlResourceMap;
	}

	/**
	 * Obtains the logger instance that is responsible for printing logging
	 * information to the server operator.
	 *
	 * @return The logger instance for this Hybrid Server.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Starts the server service thread, which binds sockets and accepts connections
	 * from clients. This method does not return until the server is ready has bound
	 * to the socket; that is, it is ready to accept connections.
	 */
	public void start() {
		synchronized (serverThreadLock) {
			if (serverThread != null) {
				return;
			}

			serverThread = new Thread() {
				@Override
				public void run() {
					logger.log(Level.INFO, "Starting server", name);

					try (final ServerSocketChannel serverSocket = ServerSocketChannel.open().bind(new InetSocketAddress(servicePort))) {
						logger.log(Level.INFO, "Listening on {0} for incoming connections", serverSocket.getLocalAddress());

						// Tell other threads we're ready to accept connections
						synchronized (serverReadyLock) {
							serverReady = true;
							serverReadyLock.notifyAll();
						}

						// Keep accepting incoming connections until other thread signals us to stop
						while (!interrupted()) {
							try (final SocketChannel clientSocket = serverSocket.accept()) {
								if (!interrupted()) {
									logger.log(Level.FINE, "Received connection from {0}", clientSocket.getRemoteAddress());

									// Handle the request
									final Socket oldIoClientSocket = clientSocket.socket();
									new HTTPRequestHandlerController(
											HybridServer.this,
											oldIoClientSocket.getInputStream(),
											oldIoClientSocket.getOutputStream()
									).handleIncoming();
								}
							} catch (final IOException exc) {
								// Report I/O exceptions, but do not report signals received by other
								// threads to stop
								if (!(exc instanceof ClosedByInterruptException)) {
									logger.log(Level.WARNING, "An I/O error occured while processing a response to a client", exc);
								}
							}
						}
					} catch (final IOException exc) {
						logger.log(Level.SEVERE, "Couldn't bind to port {0}", servicePort);
					} finally {
						// Signal ourselves being stopped by clearing the attribute which holds a reference
						// to this thread
						synchronized (serverThreadLock) {
							serverThread = null;
							serverThreadLock.notifyAll();
						}

						// We are not actually be ready to accept connections, but consider
						// ourselves ready as we don't want to make clients wait anymore
						synchronized (serverReadyLock) {
							if (!serverReady) {
								serverReady = true;
								serverReadyLock.notifyAll();
							}
						}
					}
				}
			};

			// Tell the OS scheduler to start the thread.
			// The OS will hopefully start the thread some day...
			serverThread.start();
		}

		// Wait until the server thread is ready to accept connections,
		// so users (and tests) of this method know for sure that we
		// are at least ready to attend connections
		synchronized (serverReadyLock) {
			while (!serverReady) {
				try {
					serverReadyLock.wait();
				} catch (final InterruptedException exc) {
					// Give up
					break;
				}
			}
		}
	}

	/**
	 * Stops the server orderly, so it won't accept new connections. This method
	 * waits for the server service thread to stop.
	 */
	public void stop() {
		synchronized (serverThreadLock) {
			// Signal the server thread to stop
			serverThread.interrupt();

			// Wait for the server to stop
			final long waitStart = System.currentTimeMillis();
			final long waitEnd = waitStart + STOP_WAIT_MS;
			while (serverThread != null && System.currentTimeMillis() - waitStart < STOP_WAIT_MS) {
				try {
					serverThreadLock.wait(waitEnd - System.currentTimeMillis());
				} catch (final InterruptedException | IllegalArgumentException exc) {
					// Ignore, we did our best to stop the server
				}
			}

			// Warn the user if the server is still alive
			if (serverThread != null) {
				logger.log(Level.SEVERE, "Couldn't stop the server in a timely manner");
			}
		}
	}
}
