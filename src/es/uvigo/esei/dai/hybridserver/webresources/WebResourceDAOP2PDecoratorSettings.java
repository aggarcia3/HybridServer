package es.uvigo.esei.dai.hybridserver.webresources;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.ServerConfiguration;

/**
 * Stores the settings needed to create a P2P network DAO decorator, that
 * enhances another DAO.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is not thread-safe.
 */
public class WebResourceDAOP2PDecoratorSettings implements WebResourceDAOSettings<WebResourceDAOP2PDecoratorSettings> {
	private final WebResourceDAO<?> webResourceDao;
	private final List<ServerConfiguration> remoteServers;
	private final Executor taskExecutor;
	private final Logger logger;
	private Class<? extends WebResource<?>> webResourceType = null;

	/**
	 * Creates an object which stores the settings needed to create a P2P network
	 * backed web resource DAO.
	 *
	 * @param webResourceDao The base web resource DAO, whose functionality is
	 *                       augmented by the P2P network DAO decorator.
	 *                       <p>
	 *                       <b>The generic type parameter of this DAO must be equal
	 *                       to that expected by the users of this class</b>, when
	 *                       they call
	 *                       {@link WebResourceDAOP2PDecoratorSettings#getWebResourceDAO}.
	 *                       If a mismatch happens, heap pollution will occur, and
	 *                       runtime exceptions will be thrown.
	 * @param remoteServers  A list of remote server contact details, used to
	 *                       establish communications with them as necessary.
	 * @param taskExecutor   The {@code Executor} responsible for running any
	 *                       parallel tasks that this P2P decorator needs to do.
	 * @param logger         The logger to use for outputting information about the
	 *                       DAO operations.
	 * @throws IllegalArgumentException If any parameter is {@code null}, or if
	 *                                  {@code remoteServers} contains {@code null}
	 *                                  objects.
	 */
	public WebResourceDAOP2PDecoratorSettings(
		final WebResourceDAO<?> webResourceDao, final List<ServerConfiguration> remoteServers, final Executor taskExecutor, final Logger logger
	) {
		if (webResourceDao == null) {
			throw new IllegalArgumentException(
				"Can't create settings for a P2P web resource DAO decorator with a null underlying web resource DAO"
			);
		}

		if (remoteServers == null) {
			throw new IllegalArgumentException(
				"Can't create settings for a P2P web resource DAO decorator with a null remote server list"
			);
		}

		if (taskExecutor == null) {
			throw new IllegalArgumentException(
				"Can't create settings for a P2P web resource DAO decorator with a null task executor"
			);
		}

		if (logger == null) {
			throw new IllegalArgumentException(
				"Can't create settings for a P2P web resource DAO decorator with a null logger"
			);
		}

		if (remoteServers.contains(null)) {
			throw new IllegalArgumentException(
				"Can't create settings for a P2P web resource DAO decorator with a remote server list that contains null server configurations"
			);
		}

		this.webResourceDao = webResourceDao;
		this.remoteServers = Collections.unmodifiableList(remoteServers);
		this.taskExecutor = taskExecutor;
		this.logger = logger;
	}

	@Override
	public WebResourceDAOP2PDecoratorFactory getFactory() {
		return WebResourceDAOP2PDecoratorFactory.get();
	}

	/**
	 * Returns the web resource DAO whose functionality will be augmented by the to
	 * be instantiated P2P decorator.
	 *
	 * @param <T> The type of the web resource that the augmented DAO operates upon.
	 *            The implementation of this class can't ensure type safety between
	 *            the type parameter of the DAO provided on instantiation time and
	 *            the type parameter of the DAO needed by invokers of this method,
	 *            so <b>it performs unchecked casts</b>. It is the <b>responsibility
	 *            of the caller to deal with the heap pollution that might occur, or
	 *            avoid it</b>.
	 * @return The aforementioned web resource DAO.
	 */
	@SuppressWarnings("unchecked") // Safe by contract
	public <T extends WebResource<T>> WebResourceDAO<T> getWebResourceDAO() {
		return (WebResourceDAO<T>) webResourceDao;
	}

	/**
	 * Returns a unmodifiable list of remote server configurations. Communication
	 * with these servers will be established when needed to perform a data access
	 * operation.
	 *
	 * @return The described list.
	 */
	public List<ServerConfiguration> getRemoteServers() {
		return remoteServers;
	}

	/**
	 * Returns the {@code Executor} responsible for running any parallel tasks that
	 * this P2P decorator needs to do.
	 *
	 * @return The described executor.
	 */
	public Executor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Returns the logger associated with the DAO created by this settings object,
	 * which will be used to output information about the DAO operations.
	 *
	 * @return The described logger. It can be null if no logging is desired.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Stores a reference to the type of the web resources the base DAO operates
	 * with, used by the implementation of the P2P decorator. <b>This type must
	 * match the type of the web resource the base DAO actually works with</b>.
	 * Failure to guarantee that precondition will result in heap pollution and
	 * runtime exceptions, due to the inability to check it in compilation time.
	 *
	 * @param webResourceType The described type.
	 * @throws IllegalArgumentException If {@code webResourceType} is {@code null}.
	 */
	void setWebResourceType(final Class<? extends WebResource<?>> webResourceType) {
		if (webResourceType == null) {
			throw new IllegalArgumentException(
				"The base DAO of a P2P decorator can't have a null web resource type"
			);
		}

		this.webResourceType = webResourceType;
	}

	/**
	 * Returns a reference to the type of the web resources the base DAO operates
	 * with, previously set with
	 * {@link WebResourceDAOP2PDecoratorSettings#setWebResourceType}.
	 *
	 * @param <T> The type of the web resources the base DAO operates with. See the
	 *            warning on
	 *            {@link WebResourceDAOP2PDecoratorSettings#setWebResourceType} for
	 *            information about the type safety of this parameter.
	 * @return The described type. It might be {@code null} if no type was assigned
	 *         yet.
	 * @see WebResourceDAOP2PDecoratorSettings#setWebResourceType
	 */
	@SuppressWarnings("unchecked") // Safe by contract
	<T extends WebResource<T>> Class<T> getWebResourceType() {
		return (Class<T>) webResourceType;
	}
}
