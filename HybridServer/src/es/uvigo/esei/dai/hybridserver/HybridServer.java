package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.webresource.IOBackedWebResourceMap;
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceDataOriginFactory;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceJDBCDataOriginSettings;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceMemoryDataOriginSettings;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceType;

public final class HybridServer {
	private static final Properties DEFAULT_CONFIGURATION;
	private static final Map<String, Predicate<String>> CONFIGURATION_PARAMETERS_PREDICATES;

	private final String name = "Hybrid Server";
	private final Logger logger = Logger.getLogger(name);
	private final ResourceReader resourceReader = new ResourceReader(logger);
	private final Map<WebResourceType, IOBackedWebResourceMap<String, WebResource>> webResourceMaps = new EnumMap<>(WebResourceType.class);

	private final Properties configuration;

	// Accesses to this attribute should be guarded by serverThreadLock
	private Thread serverThread = null;
	// Accesses to this attribute should be guarded by serverThreadLock
	private boolean serverThreadReady = false;
	private final Object serverThreadLock = new Object();

	static {
		DEFAULT_CONFIGURATION = new Properties();
		final Map<String, Predicate<String>> parametersPredicates = new HashMap<>(6);

		// Predicate that every accepted natural integer configuration parameter
		// must validate
		final Predicate<String> naturalIntegerPredicate = new Predicate<String>() {
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
		final Predicate<String> nonNullStringPredicate = new Predicate<String>() {
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
	 * Creates a Hybrid Server whose web resources are provided by a database, using
	 * the default configuration parameters.
	 */
	public HybridServer() {
		this.configuration = new Properties(DEFAULT_CONFIGURATION);

		initializeJDBCWebResourceMaps(
			configuration.getProperty("db.url"),
			configuration.getProperty("db.user"),
			configuration.getProperty("db.password"),
			logger
		);
	}

	/**
	 * Creates a Hybrid Server whose initial web resources are those specified in
	 * the in-memory map. Modifications of web resources will not pass-through to
	 * this map. The server will listen on the default service port, and be
	 * initialized with the default configuration parameters. This constructor is
	 * mainly useful for tests.
	 *
	 * @param htmlPages The initial HTML web resources of the server. The keys of
	 *                  the map are UUIDs, and the values the content associated to
	 *                  that UUID.
	 */
	public HybridServer(final Map<String, String> htmlPages) {
		this.configuration = new Properties(DEFAULT_CONFIGURATION);

		initializeMemoryWebResourceMaps();
		try {
			logger.log(Level.FINER, "Adding {0} HTML pages to the HTML web resource page map", htmlPages.size());

			for (Map.Entry<String, String> htmlPage : htmlPages.entrySet()) {
				webResourceMaps.get(WebResourceType.HTML).put(
					htmlPage.getKey(),
					new WebResource(htmlPage.getValue())
				);
			}
		} catch (final IOException exc) {
			// This shouldn't happen
			throw new AssertionError(exc);
		}
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

		initializeJDBCWebResourceMaps(
			properties.getProperty("db.url"),
			properties.getProperty("db.user"),
			properties.getProperty("db.password"),
			logger
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
	 * Gets the the web resource map for a kind of web resources of this Hybrid
	 * Server.
	 *
	 * @return The web resource map of the specified type of web resource for this
	 *         server.
	 */
	public IOBackedWebResourceMap<String, WebResource> getWebResourceMap(final WebResourceType webResourceType) {
		final IOBackedWebResourceMap<String, WebResource> toret = webResourceMaps.get(webResourceType);

		// Sanity check
		if (toret == null) {
			throw new AssertionError();
		}

		return toret;
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
	 * to the socket; that is, it is ready to accept connections. If the server
	 * couldn't be started because of an error, then this method returns when a best
	 * effort to start it has been made. If the server is already started, this
	 * method has no effect.
	 */
	public void start() {
		// Create a thread if we aren't already started, and wait for it to be ready
		synchronized (serverThreadLock) {
			if (serverThread == null) {
				System.out.println("-- WELCOME TO " + getName().toUpperCase() + " --");

				serverThread = new ServerThread();
				serverThread.start();

				// Wait until the server thread is ready to accept connections or stopped,
				// so users (and tests) of this method know for sure that we did our best
				// to start the server before returning the control to them
				while (serverThread != null && !serverThreadReady) {
					try {
						serverThreadLock.wait();
					} catch (final InterruptedException exc) {
						// Give up waiting
						break;
					}
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
			if (serverThread != null) {
				// Signal the server thread to stop
				serverThread.interrupt();

				// Wait for it to actually stop
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

				// Warn the operator if we didn't succeed in stopping the server
				if (serverThread != null) {
					logger.log(Level.SEVERE, "Couldn't stop the server in a timely manner");
				} else {
					// Release resources held by I/O backed maps
					try {
						for (final IOBackedWebResourceMap<?, ?> resourceMap : webResourceMaps.values()) {
							resourceMap.close();
						}
					} catch (final IOException exc) {
						logger.log(Level.WARNING, "Couldn't relinquish the resources associated to a I/O backed map", exc);
					}
				}
			}
		}
	}

	/**
	 * Checks whether the server has been started successfully and is accepting
	 * incoming connections.
	 *
	 * @return True if and only if the server has been started successfully, false
	 *         in other case.
	 */
	public boolean started() {
		synchronized (serverThreadLock) {
			return serverThread != null && serverThreadReady;
		}
	}

	/**
	 * Contains the service thread logic, which attends client requests that arrive
	 * on a stream client socket.
	 *
	 * @author Alejandro González García
	 */
	private final class ServiceThread implements Runnable {
		private final Socket clientSocket;

		/**
		 * Creates a new service thread associated to a given client socket.
		 *
		 * @param clientSocket The client socket this service thread will serve.
		 */
		public ServiceThread(final Socket clientSocket) {
			if (clientSocket == null) {
				throw new IllegalArgumentException("Can't create a service thread for a null client socket");
			}

			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try {
				new HTTPRequestHandlerController(
					HybridServer.this,
					clientSocket.getInputStream(),
					clientSocket.getOutputStream()
				).handleIncoming();

				clientSocket.close();
			} catch (final Exception exc) {
				logger.log(Level.WARNING, "An exception has occured while attending a client socket", exc);
			}
		}
	}

	/**
	 * Contains the logic for accepting stream socket connections in a separate
	 * thread.
	 *
	 * @author Alejandro González García
	 */
	private class ServerThread extends Thread {
		@Override
		public void run() {
			logger.log(Level.INFO, "Starting server thread");

			final ExecutorService executorService = Executors.newFixedThreadPool(getNumClients());

			try (final ServerSocketChannel serverSocket = ServerSocketChannel.open().bind(new InetSocketAddress(getPort()))) {
				logger.log(Level.INFO, "Listening on {0} for incoming connections", serverSocket.getLocalAddress());

				// Tell other threads we're ready to accept connections
				synchronized (serverThreadLock) {
					serverThreadReady = true;
					serverThreadLock.notifyAll();
				}

				// Keep accepting incoming connections until other thread signals us to stop
				while (!interrupted()) {
					try {
						final SocketChannel clientSocket = serverSocket.accept();

						logger.log(Level.FINE, "Received connection from {0}", clientSocket.getRemoteAddress());

						// Handle the request
						final Socket oldIoClientSocket = clientSocket.socket();

						executorService.execute(new ServiceThread(oldIoClientSocket));
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
				// Interrupt service threads
				executorService.shutdownNow();

				boolean serviceThreadsTerminated = false;
				try {
					// Wait for them to stop
					serviceThreadsTerminated = executorService.awaitTermination(
						getStopWaitTime(),
						TimeUnit.MILLISECONDS
					);
				} catch (final InterruptedException ignored) {
					// Ignore, we do the appropriate thing in the finally clause
				} finally {
					synchronized (serverThreadLock) {
						// Only consider ourselves done if the service threads
						// stopped
						if (serviceThreadsTerminated) {
							serverThread = null;
							serverThreadReady = false;
						}

						// Notify interested parties so they give up
						serverThreadLock.notifyAll();
					}
				}
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
	private int getConfigurationInteger(final String key) {
		String value = configuration.getProperty(key);

		if (value == null) {
			value = DEFAULT_CONFIGURATION.getProperty(key);
			if (value == null) {
				throw new AssertionError("Couldn't get a default integer value from the default configuration");
			}
		}

		return Integer.parseInt(value);
	}

	/**
	 * Initializes the {@code webResourceMaps} attribute with a web resource map for
	 * each web resource type, using DRAM-backed web resource maps.
	 */
	private void initializeMemoryWebResourceMaps() {
		// Get the appropriate settings for a memory data origin for web resources
		final WebResourceMemoryDataOriginSettings originSettings = new WebResourceMemoryDataOriginSettings();

		// For each resource type (HTML, ...), obtain its resource map that associates UUIDs with their contents
		for (final WebResourceType resourceType : WebResourceType.values()) {
			logger.log(Level.FINER, "Initializing DRAM-backed web resource map for resource type {0}", resourceType);

			webResourceMaps.put(resourceType, WebResourceDataOriginFactory.createWebResourceMap(originSettings));
		}
	}

	/**
	 * Initializes the {@code webResourceMaps} attribute with a web resource map for
	 * each web resource type, using relational DBMS-backed web resource maps.
	 */
	private void initializeJDBCWebResourceMaps(final String dbUrl, final String dbUser, final String dbPassword, final Logger logger) {
		for (final WebResourceType resourceType : WebResourceType.values()) {
			// Configure the JDBC data origin with the relevant settings
			final WebResourceJDBCDataOriginSettings originSettings = new WebResourceJDBCDataOriginSettings(
				dbUrl,
				dbUser,
				dbPassword,
				resourceType,
				logger
			);

			logger.log(Level.FINER, "Initializing DBMS-backed web resource map for resource type {0}", resourceType);

			// Get the associated resource map for the DB and put it in the map for resource maps
			webResourceMaps.put(resourceType, WebResourceDataOriginFactory.createWebResourceMap(originSettings));
		}
	}
}
