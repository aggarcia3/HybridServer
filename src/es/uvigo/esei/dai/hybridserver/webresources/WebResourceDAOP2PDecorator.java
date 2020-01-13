package es.uvigo.esei.dai.hybridserver.webresources;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.ServerConfiguration;
import es.uvigo.esei.dai.hybridserver.webservices.HybridServerWebService;
import es.uvigo.esei.dai.hybridserver.webservices.RemoteServerWebServiceCommunicationController;

/**
 * Extends the functionality of another web resource data access object,
 * providing P2P networking between Hybrid Servers for web resource data access
 * operations.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of web resource of the decorated DAO.
 */
final class WebResourceDAOP2PDecorator<T extends WebResource<T>> implements WebResourceDAO<T> {
	private final WebResourceDAOP2PDecoratorSettings settings;
	private final WebResourceDAO<T> webResourceDao;
	private final Class<T> webResourceType;
	private final Logger logger;

	/**
	 * Creates a P2P network backed web resource DAO, which will store and read web
	 * resources from the local server and remote servers.
	 *
	 * @param settings The settings for this DAO, used to contact other servers.
	 */
	WebResourceDAOP2PDecorator(final WebResourceDAOP2PDecoratorSettings settings) {
		if (settings == null) {
			throw new IllegalArgumentException(
				"Can't create a web resource DAO P2P decorator with null settings"
			);
		}

		this.settings = settings;
		this.webResourceDao = settings.getWebResourceDAO();
		this.webResourceType = settings.getWebResourceType();
		this.logger = settings.getLogger();

		if (webResourceType == null) {
			// This shouldn't happen due to how our factory works
			throw new AssertionError(new NullPointerException());
		}
	}

	@Override
	public T get(final UUID uuid) throws IOException {
		final List<ServerConfiguration> remoteServers = settings.getRemoteServers();

		final AttributeHolder<T> retrievedWebResource = new AttributeHolder<>(webResourceDao.get(uuid));
		final AttributeHolder<Integer> remainingServers = new AttributeHolder<>(remoteServers.size());
		final Object lookupStatusLock = new Object();

		// If the local DAO couldn't retrieve the web resource, ask other servers in parallel,
		// so we minimize latency, especially if some are down, at the cost of increased
		// bandwidth and CPU usage
		if (retrievedWebResource.get() == null && remainingServers.get() > 0) {
			final Executor taskExecutor = settings.getTaskExecutor();

			for (final ServerConfiguration remoteServerConfig : remoteServers) {

				final Runnable command = () -> {
					T resourceRetrieved = null;

					try {
						// Send the request and wait for it to complete
						resourceRetrieved = webResourceType.cast(
							RemoteServerWebServiceCommunicationController.get().executeRemoteOperation(
								(final HybridServerWebService hsws) -> {
									try {
										return HybridServerWebService.localGet(hsws, webResourceType, uuid);
									} catch (final IOException exc) {
										// Skip this server
										return null;
									}
								}, remoteServerConfig, logger
							)
						);
					} catch (final ClassCastException exc) {
						// We assume that T extends WebResource<T> erases to the same type than T' extends WebResource<T'>
						throw new AssertionError(exc);
					}

					// Update the counter and variables as appropriate
					synchronized (lookupStatusLock) {
						final int newRemainingServers = remainingServers.set(remainingServers.get() - 1);

						// Store the result if nobody retrieved it before
						if (resourceRetrieved != null && retrievedWebResource.get() == null) {
							retrievedWebResource.set(resourceRetrieved);
						}

						// Notify waiting thread if the wait has concluded
						if (newRemainingServers == 0 || resourceRetrieved != null) {
							lookupStatusLock.notify();
						}
					}
				};

				try {
					taskExecutor.execute(command);
				} catch (final RejectedExecutionException exc) {
					// Server shutting down. Proceed in this thread as a fallback
					command.run();
				}
			}

			synchronized (lookupStatusLock) {
				// Wait for the resource to be retrieved, or for all servers to respond
				try {
					while (retrievedWebResource.get() == null && remainingServers.get() > 0) {
						lookupStatusLock.wait();
					}
				} catch (final InterruptedException ignored) {}

				// Cache the result locally
				if (retrievedWebResource.get() != null && !(webResourceDao instanceof WebResourceDAOP2PDecorator)) {
					webResourceDao.put(retrievedWebResource.get());
				}
			}
		}

		synchronized (lookupStatusLock) {
			return retrievedWebResource.get();
		}
	}

	@Override
	public T localGet(final UUID uuid) throws IOException {
		return webResourceDao.get(uuid);
	}

	@Override
	public void put(final T webResource) throws IOException {
		webResourceDao.put(webResource);
	}

	@Override
	public Set<UUID> uuidSet() throws IOException {
		final Set<UUID> uuidSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
		uuidSet.addAll(webResourceDao.uuidSet());

		final List<ServerConfiguration> remoteServers = settings.getRemoteServers();
		final CountDownLatch webResourceRetrievalLatch = new CountDownLatch(remoteServers.size());

		// Ask other servers for their UUID sets, too
		final Executor taskExecutor = settings.getTaskExecutor();
		for (final ServerConfiguration remoteServerConfig : remoteServers) {

			final Runnable command = () -> {
				RemoteServerWebServiceCommunicationController.get().executeRemoteOperation(
					(final HybridServerWebService hsws) -> {
						try {
							// Although the addAll operation itself is not atomic,
							// lost updates are not a problem, because the threads
							// either write the same value to a key or have disjoint
							// key sets. We only need happens-before relationships here
							return uuidSet.addAll(
								HybridServerWebService.localUuidSet(hsws, webResourceType)
							);
						} catch (final IOException exc) {
							// Skip this server
							return false;
						}
					}, remoteServerConfig, logger
				);

				webResourceRetrievalLatch.countDown();
			};

			try {
				taskExecutor.execute(command);
			} catch (final RejectedExecutionException exc) {
				// Server shutting down. Proceed in this thread as a fallback
				command.run();
			}
		}

		// Wait for all the remote servers results
		try {
			webResourceRetrievalLatch.await();
		} catch (final InterruptedException ignored) {}

		return Collections.unmodifiableSet(uuidSet);
	}

	@Override
	public Set<UUID> localUuidSet() throws IOException {
		return webResourceDao.uuidSet();
	}

	@Override
	public boolean remove(final UUID uuid) throws IOException {
		final AttributeHolder<Boolean> webResourcesRemoved = new AttributeHolder<>(webResourceDao.remove(uuid));

		final List<ServerConfiguration> remoteServers = settings.getRemoteServers();
		final AttributeHolder<Integer> remainingServers = new AttributeHolder<>(remoteServers.size());
		final Object removalLock = new Object();

		// Tell other servers to remove it from their local databases, in parallel
		final Executor taskExecutor = settings.getTaskExecutor();
		for (final ServerConfiguration remoteServerConfig : remoteServers) {

			final Runnable command = () -> {
				final Boolean resourceRemovedResult = RemoteServerWebServiceCommunicationController.get().executeRemoteOperation(
					(final HybridServerWebService hsws) -> {
						try {
							return HybridServerWebService.localRemove(hsws, webResourceType, uuid);
						} catch (final IOException exc) {
							// Skip this server
							return false;
						}
					}, remoteServerConfig, logger
				);

				// Prevent unboxing from silently throwing NPE
				final boolean resourceRemoved = resourceRemovedResult == null ? false : resourceRemovedResult;

				// Notify whether this server removed the resource, and that this
				// thread ended its work
				synchronized (removalLock) {
					final int newRemainingServers = remainingServers.set(remainingServers.get() - 1);

					// Update removal flag
					webResourcesRemoved.set(resourceRemoved || webResourcesRemoved.get());

					// Notify waiting thread if wait has concluded
					if (resourceRemoved || newRemainingServers == 0) {
						removalLock.notify();
					}
				}
			};

			try {
				taskExecutor.execute(command);
			} catch (final RejectedExecutionException exc) {
				// Server shutting down. Proceed in this thread as a fallback
				command.run();
			}
		}

		// Wait until we either determined that no server removed the resource,
		// or at least one server removed it
		synchronized (removalLock) {
			try {
				while (!webResourcesRemoved.get() && remainingServers.get() > 0) {
					removalLock.wait();
				}
			} catch (final InterruptedException ignored) {}

			return webResourcesRemoved.get();
		}
	}

	@Override
	public boolean localRemove(final UUID uuid) throws IOException {
		return webResourceDao.remove(uuid);
	}

	@Override
	public Collection<T> webResources() throws IOException {
		final Collection<T> webResources = new ConcurrentLinkedQueue<>(
			webResourceDao.webResources()
		);

		final List<ServerConfiguration> remoteServers = settings.getRemoteServers();
		final CountDownLatch webResourceRetrievalLatch = new CountDownLatch(remoteServers.size());

		final Executor taskExecutor = settings.getTaskExecutor();
		for (final ServerConfiguration remoteServerConfig : remoteServers) {
			final Runnable command = () -> {
				RemoteServerWebServiceCommunicationController.get().executeRemoteOperation(
					(final HybridServerWebService hsws) -> {
						try {
							// This operation is not atomic, but concurrent. Resources of
							// a server may be mixed with resources of another in the resulting
							// queue
							return webResources.addAll(
								HybridServerWebService.localWebResources(hsws, webResourceType)
							);
						} catch (final IOException exc) {
							// Skip this server
							return false;
						}
					}, remoteServerConfig, logger
				);

				webResourceRetrievalLatch.countDown();
			};

			try {
				taskExecutor.execute(command);
			} catch (final RejectedExecutionException exc) {
				// Server shutting down. Proceed in this thread as a fallback
				command.run();
			}
		}

		// Wait for all the remote servers results
		try {
			webResourceRetrievalLatch.await();
		} catch (final InterruptedException ignored) {}

		return Collections.unmodifiableCollection(webResources);
	}

	@Override
	public Collection<T> localWebResources() throws IOException {
		return webResourceDao.webResources();
	}

	@Override
	public void close() throws IOException {
		webResourceDao.close();
	}

	/**
	 * Utility class that holds an attribute, used to work around the restriction of
	 * final local variables for lambda expressions and anonymous classes inside a
	 * method.
	 *
	 * @author Alejandro González García
	 *
	 * @param <V> The type of the attribute that will be stored.
	 * @implNote The implementation of this class is not thread-safe by itself.
	 *           External synchronization is needed if several threads read and
	 *           update the attribute.
	 */
	private static final class AttributeHolder<V> {
		private V attribute;

		/**
		 * Creates a new attribute holder, holding the specified value.
		 *
		 * @param attribute The value of the attribute to hold.
		 */
		public AttributeHolder(final V attribute) {
			this.attribute = attribute;
		}

		/**
		 * Gets the value of the stored attribute.
		 *
		 * @return The attribute value.
		 */
		public V get() {
			return attribute;
		}

		/**
		 * Sets the value of the stored attribute.
		 *
		 * @param attribute The value to set.
		 * @return The value of {@code attribute}.
		 */
		public V set(final V attribute) {
			this.attribute = attribute;
			return attribute;
		}
	}
}
