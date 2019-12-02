package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.webresource.HTMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.JDBCWebResourceDAOFactory;
import es.uvigo.esei.dai.hybridserver.webresource.JDBCWebResourceDAOSettings;
import es.uvigo.esei.dai.hybridserver.webresource.MemoryWebResourceDAOFactory;
import es.uvigo.esei.dai.hybridserver.webresource.MemoryWebResourceDAOSettings;
import es.uvigo.esei.dai.hybridserver.webresource.WebResource;
import es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAO;
import es.uvigo.esei.dai.hybridserver.webresource.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;

/**
 * Models the Hybrid Server instance as a whole, acting as a high level
 * controller for the server functionality and runtime configuration information
 * expert.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class HybridServer {
	private static final String NAME = "Hybrid Server";

	private final Logger logger = Logger.getLogger(NAME);

	private final StaticResourceReader staticResourceReader = new StaticResourceReader(logger);
	private final Map<Class<? extends WebResource<?>>, WebResourceDAO<? extends WebResource<?>>> webResourcesMap;
	private final Configuration configuration;
	private final AtomicReference<HybridServerThread> serverThread = new AtomicReference<>();

	{
		this.webResourcesMap = new HashMap<>();
	}

	/**
	 * Creates a Hybrid Server whose web resources are provided by a database, using
	 * the default configuration parameters.
	 */
	public HybridServer() {
		this.configuration = new Configuration();

		initializeJDBCBackedDAO();
	}

	/**
	 * Constructs a Hybrid Server that will serve requests from a database. The
	 * configuration parameters, which specify the necessary information to connect
	 * to the database, are read from the provided {@link Configuration} object.
	 *
	 * @param configuration The {@link Configuration} object which contains all the
	 *                      configuration parameters for this server.
	 * @throws IllegalArgumentException If the {@code configuration} parameter is
	 *                                  {@code null}.
	 */
	public HybridServer(final Configuration configuration) {
		this.configuration = configuration;

		initializeJDBCBackedDAO();
	}

	/**
	 * Constructs a Hybrid Server that will serve requests from a database. The
	 * configuration parameters, which specify the necessary information to connect
	 * to the database, are read from the provided {@link Properties} object.
	 *
	 * @param properties The Properties object which contains all the configuration
	 *                   parameters for this server.
	 * @throws IllegalArgumentException If some setting contained in
	 *                                  {@code properties} is invalid.
	 * @deprecated Using {@link Properties} objects for setting server parameters is
	 *             discouraged, as their format was not extended to be compatible
	 *             with the P2P system, so the server will never operate in P2P mode
	 *             when using this constructor. Moreover, XML-based configuration is
	 *             technically superior in several ways. This constructor is
	 *             provided for backwards compatibility only.
	 */
	@Deprecated
	public HybridServer(final Properties properties) {
		try {
			this.configuration = new PropertiesConfigurationLoader().load(properties);
		} catch (final Exception exc) {
			throw new IllegalArgumentException(
				"The specified configuration properties are invalid", exc
			);
		}

		initializeJDBCBackedDAO();
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
	@SuppressWarnings("unchecked") // No heap pollution occurs due to how the map is initialized
	public HybridServer(final Map<String, String> htmlPages) {
		this.configuration = new Configuration();

		initializeMemoryBackedDAO();

		try {
			logger.log(Level.FINER, "Adding {0} HTML pages to the HTML web resources collection", htmlPages.size());

			for (final Entry<String, String> htmlPage : htmlPages.entrySet()) {
				try {
					((WebResourceDAO<HTMLWebResource>) webResourcesMap.get(HTMLWebResource.class)).put(
						new HTMLWebResource(
							UUID.fromString(htmlPage.getKey()), htmlPage.getValue()
						)
					);
				} catch (final IllegalArgumentException exc) {
					logger.log(
						Level.WARNING,
						"\"{0}\" is not a valid UUID, so it was not added to the web resources of the server",
						htmlPage.getKey()
					);
				}
			}
		} catch (final IOException exc) {
			// This shouldn't happen
			throw new AssertionError(exc);
		}
	}

	/**
	 * Returns the user-friendly name of this server.
	 *
	 * @return The user-friendly name of this server. Currently, "Hybrid Server".
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * Gets the configuration that this server is using.
	 *
	 * @return The aforementioned configuration.
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets the port this server will listen on, if it was not yet started, or is
	 * listening on, if it was started successfully.
	 *
	 * @return The port this server will bind a socket on, for clients to connect
	 *         to.
	 * @deprecated The method {@link HybridServer#getConfiguration} provides access
	 *             to this and other configuration parameters in a more generic way.
	 *             This method is only provided for backward compatibility with the
	 *             first release tests.
	 */
	@Deprecated
	public int getPort() {
		return configuration.getHttpPort();
	}

	/**
	 * Gets the the web resource DAO for a kind of web resources of this Hybrid
	 * Server.
	 *
	 * @param <T>             The type of web resource to get its DAO of.
	 * @param webResourceType The {@link Class} object that the desired web
	 *                        resources are a instance of.
	 * @return The web resource DAO of the specified type of web resource for this
	 *         server. If this server doesn't serve that kind of resource, the
	 *         result will be {@code null}.
	 */
	@SuppressWarnings("unchecked") // No heap pollution occurs due to how the map is initialized
	public <T extends WebResource<T>> WebResourceDAO<T> getWebResourceDAO(final Class<T> webResourceType) {
		return (WebResourceDAO<T>) webResourcesMap.get(webResourceType);
	}

	/**
	 * Obtains the resource reader to use to read static Java resources for this
	 * server.
	 *
	 * @return The described resource reader.
	 */
	public StaticResourceReader getStaticResourceReader() {
		return staticResourceReader;
	}

	/**
	 * Obtains the logger instance that is responsible for printing logging
	 * information to the server operator. This instance is constant during
	 * the lifetime of the application.
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
		System.out.println("-- WELCOME TO " + getName().toUpperCase() + " --");

		final HybridServerThread serverThread = this.serverThread.updateAndGet(
			(final HybridServerThread thread) -> {
				return thread == null || thread.getState() == State.TERMINATED ?
					new HybridServerThread(this) : thread
				; 
			}
		);

		// Only start the thread if it is new
		if (serverThread.getState() == State.NEW) {
			serverThread.start();

			// Wait until the server thread is ready to accept connections or stopped,
			// so users (and tests) of this method know for sure that we did our best
			// to start the server before returning the control to them
			final Object readyLock = serverThread.getStateLock();
			synchronized (readyLock) {
				while (!serverThread.isReady() && !serverThread.isPortInUse()) {
					try {
						readyLock.wait();
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
		final HybridServerThread serverThread = this.serverThread.get();

		if (serverThread != null) {
			serverThread.interrupt();

			// Wait for it to actually stop
			final long stopWaitTime = getStopWaitSeconds() * 1000;
			final long waitStart = System.currentTimeMillis();
			final long waitEnd = waitStart + stopWaitTime;

			final Object readyLock = serverThread.getStateLock();
			boolean stopFailed = false;
			synchronized (readyLock) {
				while (serverThread.isReady() && System.currentTimeMillis() - waitStart < stopWaitTime) {
					try {
						readyLock.wait(waitEnd - System.currentTimeMillis());
					} catch (final InterruptedException | IllegalArgumentException exc) {
						// Ignore, we did our best to stop the server
					}
				}

				// Warn the operator if we didn't succeed in stopping the server
				if (serverThread.isReady()) {
					logger.log(Level.SEVERE, "Couldn't stop the server in a timely manner");
					stopFailed = true;
				}
			}

			// Release resources held by data access objects
			if (!stopFailed) {
				try {
					for (final WebResourceDAO<?> webResourceDao : webResourcesMap.values()) {
						webResourceDao.close();
					}
				} catch (final Exception exc) {
					logger.log(Level.WARNING, "Couldn't relinquish the resources associated to a web resource DAO", exc);
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
		final HybridServerThread serverThread = this.serverThread.get();
		return serverThread != null && serverThread.isAlive() && serverThread.isReady();
	}

	/**
	 * Returns the maximum number of seconds that the server will wait for its
	 * worker threads to stop, when ordered to.
	 *
	 * @return The described number of seconds.
	 */
	int getStopWaitSeconds() {
		return 30;
	}

	/**
	 * Initializes memory-backed data access objects for the web resources this
	 * server works with.
	 */
	private void initializeMemoryBackedDAO() {
		final MemoryWebResourceDAOFactory daoFactory = MemoryWebResourceDAOFactory.get();
		final MemoryWebResourceDAOSettings settings = new MemoryWebResourceDAOSettings();

		webResourcesMap.put(HTMLWebResource.class, daoFactory.createDAO(settings, HTMLWebResource.class));
		webResourcesMap.put(XMLWebResource.class, daoFactory.createDAO(settings, XMLWebResource.class));
		webResourcesMap.put(XSDWebResource.class, daoFactory.createDAO(settings, XSDWebResource.class));
		webResourcesMap.put(XSLTWebResource.class, daoFactory.createDAO(settings, XSLTWebResource.class));
	}

	/**
	 * Initializes JDBC-backed data access objects for the web resources this
	 * server works with.
	 */
	private void initializeJDBCBackedDAO() {
		final JDBCWebResourceDAOFactory daoFactory = JDBCWebResourceDAOFactory.get();
		final JDBCWebResourceDAOSettings settings = new JDBCWebResourceDAOSettings(
			configuration.getDbURL(), configuration.getDbUser(), configuration.getDbPassword(), logger
		);

		webResourcesMap.put(HTMLWebResource.class, daoFactory.createDAO(settings, HTMLWebResource.class));
		webResourcesMap.put(XMLWebResource.class, daoFactory.createDAO(settings, XMLWebResource.class));
		webResourcesMap.put(XSDWebResource.class, daoFactory.createDAO(settings, XSDWebResource.class));
		webResourcesMap.put(XSLTWebResource.class, daoFactory.createDAO(settings, XSLTWebResource.class));
	}
}
