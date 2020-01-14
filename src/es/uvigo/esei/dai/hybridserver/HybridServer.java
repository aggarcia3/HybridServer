package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.lang.Thread.State;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.pools.JDBCConnectionPool;
import es.uvigo.esei.dai.hybridserver.webresources.HTMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.JDBCWebResourceDAOFactory;
import es.uvigo.esei.dai.hybridserver.webresources.JDBCWebResourceDAOSettings;
import es.uvigo.esei.dai.hybridserver.webresources.MemoryWebResourceDAOFactory;
import es.uvigo.esei.dai.hybridserver.webresources.MemoryWebResourceDAOSettings;
import es.uvigo.esei.dai.hybridserver.webresources.WebResource;
import es.uvigo.esei.dai.hybridserver.webresources.WebResourceDAO;
import es.uvigo.esei.dai.hybridserver.webresources.WebResourceDAOP2PDecoratorFactory;
import es.uvigo.esei.dai.hybridserver.webresources.WebResourceDAOP2PDecoratorSettings;
import es.uvigo.esei.dai.hybridserver.webresources.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresources.XSLTWebResource;

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
	private final Map<String, String> sampleHtmlPages;
	private final Consumer<Map<String, String>> daoInitializer;

	private final StaticResourceReader staticResourceReader = new StaticResourceReader(logger);
	private final Map<Class<? extends WebResource<?>>, WebResourceDAO<? extends WebResource<?>>> webResourcesMap;
	private final Configuration configuration;
	private final AtomicReference<HybridServerThread> serverThread = new AtomicReference<>();

	private volatile ExecutorService executorService = null;

	{
		this.webResourcesMap = new ConcurrentHashMap<>(4); // 4 is the number of web resource types
	}

	/**
	 * Creates a Hybrid Server whose web resources are provided by a database, using
	 * the default configuration parameters.
	 */
	public HybridServer() {
		this.configuration = new Configuration();
		this.daoInitializer = JDBCBackedDAOInitializer();
		this.sampleHtmlPages = Collections.emptyMap();
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
		this.daoInitializer = JDBCBackedDAOInitializer();
		this.sampleHtmlPages = Collections.emptyMap();
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

		this.daoInitializer = JDBCBackedDAOInitializer();
		this.sampleHtmlPages = Collections.emptyMap();
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
		this.configuration = new Configuration();

		this.daoInitializer = memoryBackedDAOInitializer();
		this.sampleHtmlPages = Collections.unmodifiableMap(htmlPages);
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
		System.out.println("-- WELCOME TO " + NAME.toUpperCase() + " --");

		final HybridServerThread serverThread = this.serverThread.updateAndGet(
			(final HybridServerThread thread) -> {
				return thread == null || thread.getState() == State.TERMINATED ?
					new HybridServerThread(this) : thread
				; 
			}
		);

		// Only start the thread if it is new
		if (serverThread.getState() == State.NEW) {
			// Initialize the executor service
			initializeExecutorService();

			// Initialize the DAO
			daoInitializer.accept(sampleHtmlPages);

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
	 * whatever occurs first. Callers should not invoke the
	 * {@link HybridServer#start} method until this one returns; failure to do so
	 * may imply that a new server thread can't be initialized properly.
	 */
	public void stop() {
		final HybridServerThread serverThread = this.serverThread.getAndUpdate(
			(final HybridServerThread t) -> null
		);

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

			// Remove the DAO from the map, in case the server is restarted
			final Iterator<WebResourceDAO<? extends WebResource<?>>> iter = webResourcesMap.values().iterator();
			while (iter.hasNext()) {
				final WebResourceDAO<?> webResourceDao = iter.next();
				iter.remove();

				try {
					// Release their resources if they are no longer used.
					// The following condition will evaluate to true under normal conditions.
					// If it doesn't evaluate to true, things are so messed up that leaking
					// resources is probably the least of the concerns
					if (!stopFailed) {
						webResourceDao.close();
					}
				} catch (final IOException exc) {
					logger.log(Level.WARNING, "Couldn't relinquish the resources associated to a web resource DAO", exc);
				}
			}

			logger.log(Level.INFO, "Server stopped");
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
	 * Returns the executor service responsible for running the worker threads of
	 * this Hybrid Server. Users of this method are responsible for not performing
	 * any activity on the returned object that might render the server unusable
	 * (i.e. unable to submit tasks to this executor while started). Conversely, the
	 * returned executor service might be {@code null} or shutdown if the server is
	 * not started, or while it is stopping.
	 *
	 * @return The described executor service.
	 */
	public ExecutorService getExecutorService() {
		return executorService;
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
	 * Initializes the executor service to be used by this Hybrid Server. This
	 * method assumes that the configuration for the server has been read.
	 */
	private void initializeExecutorService() {
		this.executorService = Executors.newFixedThreadPool(configuration.getNumClients());
	}

	/**
	 * Returns a consumer that initializes memory-backed data access objects for the
	 * web resources this server works with.
	 *
	 * @return The described consumer.
	 */
	private Consumer<Map<String, String>> memoryBackedDAOInitializer() {
		return (final Map<String, String> htmlPages) -> {
			final MemoryWebResourceDAOFactory daoFactory = MemoryWebResourceDAOFactory.get();
			final MemoryWebResourceDAOSettings settings = new MemoryWebResourceDAOSettings();

			webResourcesMap.put(HTMLWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, HTMLWebResource.class), HTMLWebResource.class)
			);
			webResourcesMap.put(XMLWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XMLWebResource.class), XMLWebResource.class)
			);
			webResourcesMap.put(XSDWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XSDWebResource.class), XSDWebResource.class)
			);
			webResourcesMap.put(XSLTWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XSLTWebResource.class), XSLTWebResource.class)
			);

			putSampleHtmlPages(htmlPages);
		};
	}

	/**
	 * Returns a consumer that initializes JDBC-backed data access objects for the
	 * web resources this server works with.
	 *
	 * @return The described consumer.
	 */
	private Consumer<Map<String, String>> JDBCBackedDAOInitializer() {
		return (final Map<String, String> htmlPages) -> {
			final JDBCWebResourceDAOFactory daoFactory = JDBCWebResourceDAOFactory.get();

			final JDBCConnectionPool dbConnectionPool = new JDBCConnectionPool(
				configuration.getNumClients(),
				() -> DriverManager.getConnection(
					configuration.getDbURL(), configuration.getDbUser(), configuration.getDbPassword()
				), logger
			);

			final JDBCWebResourceDAOSettings settings = new JDBCWebResourceDAOSettings(
				dbConnectionPool, logger
			);

			webResourcesMap.put(HTMLWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, HTMLWebResource.class), HTMLWebResource.class)
			);
			webResourcesMap.put(XMLWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XMLWebResource.class), XMLWebResource.class)
			);
			webResourcesMap.put(XSDWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XSDWebResource.class), XSDWebResource.class)
			);
			webResourcesMap.put(XSLTWebResource.class, decorateDAOWithP2PNetworking(
				daoFactory.createDAO(settings, XSLTWebResource.class), XSLTWebResource.class)
			);

			putSampleHtmlPages(htmlPages);
		};
	}

	/**
	 * Adds the specified sample HTML pages to the HTML web resource DAO, previously
	 * initialized.
	 *
	 * @param htmlPages The map of HTML pages to add, where the key is the UUID and
	 *                  the value its content.
	 */
	@SuppressWarnings("unchecked") // No heap pollution occurs due to how the map is initialized
	private void putSampleHtmlPages(final Map<String, String> htmlPages) {
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
	 * Decorates a base DAO with P2P networking capabilities if needed, so that the
	 * returned DAO will cooperate with remote Hybrid Servers (if any) for data
	 * access operations.
	 *
	 * @param <T>             The type of the web resource that the base DAO
	 *                        operates with.
	 * @param baseDao         The DAO to augment its functionality.
	 * @param webResourceType A class instances that represents the type of the web
	 *                        resource that the base DAO operates with.
	 * @return The augmented DAO, with P2P networking capabilities.
	 */
	private <T extends WebResource<T>> WebResourceDAO<T> decorateDAOWithP2PNetworking(
		final WebResourceDAO<T> baseDao, final Class<T> webResourceType
	) {
		final List<ServerConfiguration> remoteServers = configuration.getServers();

		// Do not decorate if there are no remote servers
		return remoteServers.isEmpty() ?
			baseDao :
			WebResourceDAOP2PDecoratorFactory.get()
			.createDAO(
				new WebResourceDAOP2PDecoratorSettings(baseDao, remoteServers, executorService, logger), webResourceType
			)
		;
	}
}
