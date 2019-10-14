package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.adt.IOBackedMap;
import es.uvigo.esei.dai.hybridserver.adt.JDBCBackedHTMLResourceMap;
import es.uvigo.esei.dai.hybridserver.adt.MemoryBackedHTMLResourceMap;

public final class HybridServer {
	private static final Properties DEFAULT_CONFIGURATION;
	private static final Map<String, Predicate<String>> CONFIGURATION_PARAMETERS_PREDICATES;

	private final String name = "Hybrid Server";
	private final Logger logger = Logger.getLogger(name);
	private final ResourceReader resourceReader = new ResourceReader(this);
	private final IOBackedMap<String, String> htmlResourceMap;

	private final Properties configuration;

	// Accesses to this variable should be guarded by serverThreadLock
	private Thread serverThread = null;
	// Accesses to this variable should be guarded by serverReadyLock
	private boolean serverReady = false;
	private final Object serverThreadLock = new Object();
	private final Object serverReadyLock = new Object();

	static {
		DEFAULT_CONFIGURATION = new Properties();
		final Map<String, Predicate<String>> parametersPredicates = new HashMap<>(6);

		// Predicate that every accepted natural integer configuration parameter
		// must validate
		final Predicate<String> naturalIntegerPredicate = new Predicate<>() {
			@Override
			public boolean test(final String value) {
				try {
					int intValue = Integer.parseInt(value);

					// Only positive integers greater than 0 are valid
					if (intValue < 1) {
						throw new NumberFormatException();
					}

					return true;
				} catch (final NumberFormatException exc) {
					return false;
				}
			}
		};

		// Predicate that every non-null parameter must validate
		final Predicate<String> nonNullStringPredicate = new Predicate<>() {
			@Override
			public boolean test(final String value) {
				return value != null;
			}
		};

		// Actual configuration parameters, with their default values and
		// validation predicates
		DEFAULT_CONFIGURATION.setProperty("port", Integer.toString(8888));
		parametersPredicates.put("port", naturalIntegerPredicate);

		DEFAULT_CONFIGURATION.setProperty("stopWaitTime", Integer.toString(10000));
		parametersPredicates.put("stopWaitTime", naturalIntegerPredicate);

		DEFAULT_CONFIGURATION.setProperty("numClients", Integer.toString(50));
		parametersPredicates.put("numClients", naturalIntegerPredicate);

		DEFAULT_CONFIGURATION.setProperty("db.url", "jdbc:mysql://localhost:3306/hstestdb");
		parametersPredicates.put("db.url", nonNullStringPredicate);

		DEFAULT_CONFIGURATION.setProperty("db.user", "hsdb");
		parametersPredicates.put("db.user", nonNullStringPredicate);

		DEFAULT_CONFIGURATION.setProperty("db.password", "hsdbpass");
		parametersPredicates.put("db.password", nonNullStringPredicate);

		// Wrap the map as an unmodifiable one to better state our vision
		// of it being read-only after initialization
		CONFIGURATION_PARAMETERS_PREDICATES = Collections.unmodifiableMap(parametersPredicates);
	}

	/**
	 * Creates a Hybrid Server without initial HTML resources, that stores new ones
	 * in memory and will listen on the default service port and is initialized with
	 * default configuration parameters. This constructor is mainly useful for
	 * tests.
	 */
	public HybridServer() {
		this.htmlResourceMap = new MemoryBackedHTMLResourceMap();
		this.configuration = new Properties(DEFAULT_CONFIGURATION);
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

		this.configuration = new Properties(DEFAULT_CONFIGURATION);
	}

	/**
	 * Constructs a Hybrid Server that will serve requests from a database, with
	 * modifiable configuration parameters. The configuration parameters, which
	 * specify the necessary information to connect to the database, are read from a
	 * Properties object. Absent or invalid values will be set to default ones, and
	 * a warning will be issued for the operator to review and (ideally) act upon.
	 *
	 * @param properties The Properties object which contains all the configuration
	 *                   parameters for this server.
	 */
	public HybridServer(final Properties properties) {
		if (properties == null) {
			throw new IllegalArgumentException("Can't read configuration options for a server from a null properties object");
		}

		// Validate parameters, initializing the missing ones to default values
		for (final Map.Entry<String, Predicate<String>> configurationEntry : CONFIGURATION_PARAMETERS_PREDICATES.entrySet()) {
			final String key = configurationEntry.getKey();
			final String readValue = properties.getProperty(key);

			if (readValue == null || !configurationEntry.getValue().test(readValue)) {
				final String defaultValue = DEFAULT_CONFIGURATION.getProperty(key);

				logger.log(Level.WARNING,
						readValue == null ?
							"The configuration value for the parameter \"{0}\" is absent. Using the default value of \"{1}\"" :
							"The configuration value for the parameter \"{0}\" is not valid. Using the default value of \"{1}\"",
						new Object[] { key, defaultValue }
				);

				properties.setProperty(key, defaultValue);
			}
		}

		this.configuration = properties;
		this.htmlResourceMap = new JDBCBackedHTMLResourceMap(
			properties.getProperty("db.url"),
			properties.getProperty("db.user"),
			properties.getProperty("db.password")
		);
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
	 * listening on, if it was started successfully.
	 *
	 * @return The port this server will bind a socket on, for clients to connect to.
	 */
	public int getPort() {
		return getConfigurationInteger("port");
	}

	/**
	 * Obtains the maximum number of clients that this server will respond to in
	 * parallel.
	 *
	 * @return The described number.
	 */
	public int getNumClients() {
		return getConfigurationInteger("numClients");
	}

	/**
	 * Gets the maximum time that a call to {@link HybridServer#stop} will block
	 * waiting for the main server thread to stop.
	 *
	 * @return The described time, in milliseconds.
	 */
	protected int getStopWaitTime() {
		return getConfigurationInteger("stopWaitTime");
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

					try (final ServerSocketChannel serverSocket = ServerSocketChannel.open().bind(new InetSocketAddress(getPort()))) {
						logger.log(Level.INFO, "Listening on {0} for incoming connections", serverSocket.getLocalAddress());

						// Tell other threads we're ready to accept connections
						synchronized (serverReadyLock) {
							serverReady = true;
							serverReadyLock.notifyAll();
						}

						// Keep accepting incoming connections until other thread signals us to stop
						while (!interrupted()) {
							try (final SocketChannel clientSocket = serverSocket.accept()) {
								logger.log(Level.FINE, "Received connection from {0}", clientSocket.getRemoteAddress());

								// Handle the request
								final Socket oldIoClientSocket = clientSocket.socket();
								new HTTPRequestHandlerController(
										HybridServer.this,
										oldIoClientSocket.getInputStream(),
										oldIoClientSocket.getOutputStream()
								).handleIncoming();
							} catch (final IOException exc) {
								// Report I/O exceptions, but do not report signals received by other
								// threads to stop
								if (!(exc instanceof ClosedByInterruptException)) {
									logger.log(Level.WARNING, "An I/O error occured while processing a response to a client", exc);
								}
							}
						}
					} catch (final IOException exc) {
						logger.log(Level.SEVERE, "Couldn't bind to port {0}", getPort());
					} finally {
						// Signal ourselves being stopped by clearing the attribute which holds a reference
						// to this thread
						synchronized (serverThreadLock) {
							serverThread = null;
							serverThreadLock.notifyAll();
						}

						// We are not actually be ready to accept connections, but consider
						// ourselves ready no matter what as we don't want clients to wait anymore
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
			// The OS will start the thread some day...
			serverThread.start();
		}

		// Wait until the server thread is ready to accept connections,
		// so users (and tests) of this method know for sure that we
		// are at least ready to process incoming requests
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
	 * waits until the main server thread stops or a maximum wait time has elapsed,
	 * whatever occurs first.
	 */
	public void stop() {
		synchronized (serverThreadLock) {
			// Signal the server thread to stop
			serverThread.interrupt();

			// Wait for the server to stop
			final long waitStart = System.currentTimeMillis();
			final int stopWaitTime = getStopWaitTime();
			final long waitEnd = waitStart + stopWaitTime;
			while (serverThread != null && System.currentTimeMillis() - waitStart < stopWaitTime) {
				try {
					serverThreadLock.wait(waitEnd - System.currentTimeMillis());
				} catch (final InterruptedException | IllegalArgumentException exc) {
					// Ignore, we did our best to stop the server
				}
			}

			// Warn the user if the server is still alive
			if (serverThread != null) {
				logger.log(Level.WARNING, "Couldn't stop the server in a timely manner");
			}
		}
	}

	/**
	 * Gets the integer value associated to the specified key in the configuration.
	 * The implementation assumes that if the key is present in the loaded
	 * configuration or the default configuration its value is correct.
	 *
	 * @param key The key to get its associated integer of.
	 * @return The integer associated to the configuration key.
	 */
	protected int getConfigurationInteger(final String key) {
		String value = configuration.getProperty(key);

		if (value == null) {
			value = DEFAULT_CONFIGURATION.getProperty(key);
			if (value == null) {
				throw new AssertionError("Couldn't get a default integer value from the default configuration");
			}
		}

		return Integer.parseInt(value);
	}
}
