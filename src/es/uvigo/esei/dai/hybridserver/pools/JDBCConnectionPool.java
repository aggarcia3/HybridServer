package es.uvigo.esei.dai.hybridserver.pools;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains a pool of JDBC connections, with a configurable high water mark and
 * connection supplier, that is initially empty. Providing that the underlying
 * connection supplier and database JDBC operations do not throw exceptions, the
 * pool will always return a ready to use {@code Connection} object to
 * interested threads. This implies that, if no {@code Connection}s remain in
 * the pool, new {@code Connection}s will be allocated. However, excess
 * {@code Connection}s that would make the pool bigger than its high water mark
 * will be closed and discarded when they are returned. Therefore, an
 * appropriate high water mark is crucial for performance.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class JDBCConnectionPool implements Closeable {
	private static final int LOGIN_TIMEOUT_SECONDS = 30;

	private final int maximumSize;
	private final Queue<Connection> connectionsQueue;
	private final Callable<Connection> connectionSupplier;
	private final Logger logger;

	private final Object queueLock = new Object();
	private boolean isClosed = false;

	/**
	 * Creates a new JDBC connection pool.
	 *
	 * @param maximumSize        The maximum size (i.e. high water mark) of the
	 *                           collection of {@code Connection}s internally used
	 *                           by this pool. It must be greater than 0.
	 * @param connectionSupplier A task that will provide the pool with whatever new
	 *                           {@code Connection} objects it needs. It must not
	 *                           return {@code null}.
	 * @param logger             The logger to log the pool operations into.
	 * @throws IllegalArgumentException If some parameter is {@code null} or
	 *                                  invalid.
	 */
	public JDBCConnectionPool(final int maximumSize, final Callable<Connection> connectionSupplier, final Logger logger) {
		if (maximumSize < 1) {
			throw new IllegalArgumentException("The pool size must be greater than 0");
		}

		if (connectionSupplier == null || logger == null) {
			throw new IllegalArgumentException(
				"A JDBC connection pool can't be created with null arguments"
			);
		}

		this.maximumSize = maximumSize;
		this.connectionsQueue = new LinkedList<>();
		this.connectionSupplier = connectionSupplier;
		this.logger = logger;
	}

	/**
	 * Retrieves a JDBC connection from this pool, instantiating it if and only if
	 * no {@code Connection}s remain in the pool. Otherwise, an existing instance is
	 * returned. The caller of this method must return the object back to the pool
	 * as soon as possible via {@link JDBCConnectionPool#yield}. If the pool is
	 * closed, this method behaves as if it was empty.
	 * <p>
	 * <b>Users of this method must keep the same visible state of the returned
	 * {@code Connection}</b>. Failure to do so may affect the behavior of other
	 * threads, or this same thread in the future, if they end up using the same
	 * object.
	 *
	 * @return The described connection.
	 * @throws SQLException If establishing a JDBC connection is needed, and a SQL
	 *                      error occurs during the process.
	 * @see JDBCConnectionPool#close
	 */
	public final Connection take() throws SQLException {
		Connection connection;
		boolean connectionAlive = false;
		boolean poolClosed;

		// Try to get a connection from the queue
		synchronized (queueLock) {
			poolClosed = isClosed;
			connection = poolClosed ? null : connectionsQueue.poll();
		}

		if (connection == null) {
			logger.log(
				Level.FINE,
				!poolClosed ?
					"The JDBC connection pool couldn't retrieve a connection for a thread. Establishing a new connection..." :
					"A thread wants a JDBC connection from a pool that is closed. This is not abnormal, but it should be avoided"
			);
		}

		// Check connection status. Assume it's dead if its validity can't be assured
		try {
			connectionAlive = connection != null && connection.isValid(LOGIN_TIMEOUT_SECONDS);

			// Try to close the invalid connection, although it will very likely be pointless
			if (connection != null && !connectionAlive) {
				closeConnection(connection);
			}
		} catch (final SQLException ignored) {}

		connection = connectionAlive ? connection : establishConnection();

		return connection;
	}

	/**
	 * Returns a previously took JDBC connection to the pool, making it available
	 * for retrieval by this thread or another threads via
	 * {@link JDBCConnectionPool#take}. If the pool is closed, or it has exceeded
	 * its high water mark, the connection will be closed and not returned back to
	 * the pool. Returning a {@code null} JDBC connection effectively has no effect.
	 * <p>
	 * <b>After invoking this method, callers must not use the yielded object
	 * anymore</b>. In fact, the object might be in use by another thread when the
	 * invocation returns.
	 *
	 * @param connection The connection to return back to the pool.
	 * @see JDBCConnectionPool#close
	 * @see JDBCConnectionPool#take
	 */
	public final void yield(final Connection connection) {
		boolean connectionAlive = false;

		try {
			connectionAlive = connection != null && connection.isValid(LOGIN_TIMEOUT_SECONDS);
		} catch (final SQLException ignored) {}

		boolean shouldClose;
		synchronized (queueLock) {
			shouldClose = isClosed || !connectionAlive || connectionsQueue.size() >= maximumSize;

			if (!shouldClose) {
				connectionsQueue.add(connection);
			}
		}

		if (shouldClose && connection != null) {
			// The pool normally doesn't close connections, as we've removed
			// it from the queue before and there's room for them. However, we might
			// have created extra connections if another thread was using the last one,
			// the existing ones became invalid, or the pool was closed. If that's the
			// case, silently discard it
			try {
				closeConnection(connection);
			} catch (final SQLException ignored) {
				// Upper layers won't handle this exception better anyway
			}
		}
	}

	/**
	 * Closes the pool, closing and removing all the references to
	 * {@code Connection}s stored in it. A closed pool always instantiates objects
	 * on demand, and always discards yielded connections. This behavior is intended
	 * to allow users of this class to stop their operations gracefully, so when
	 * this method returns it is not guaranteed that no one can use this pool
	 * anymore. If that is desired, an external synchronization mechanism is needed.
	 * <p>
	 * As documented in {@link Closeable}, closing an already closed pool has no
	 * effect.
	 * <p>
	 * Even if a {@code IOException} has been thrown, the pool will be empty after
	 * this method returns.
	 */
	@Override
	public final void close() throws IOException {
		SQLException firstException = null;

		synchronized (queueLock) {
			isClosed = true;

			final Iterator<Connection> iter = connectionsQueue.iterator();
			while (iter.hasNext()) {
				final Connection connection = iter.next();

				try {
					closeConnection(connection);
				} catch (final SQLException exc) {
					if (firstException != null) {
						firstException.setNextException(exc);
					} else {
						firstException = exc;
					}
				}

				// If the connection wasn't closed successfully, it's probably
				// better to try to reopen it later anyway
				iter.remove();
			}
		}

		// Delay throwing exceptions until a best effort to close all the connections
		// has been made
		if (firstException != null) {
			throw new IOException(firstException);
		}
	}

	/**
	 * Establishes a new JDBC connection by calling the connection supplier of this
	 * pool, and returns the result.
	 *
	 * @return The established connection.
	 * @throws SQLException If some SQL error occurs during the operation.
	 */
	private final Connection establishConnection() throws SQLException {
		try {
			final Connection connection = connectionSupplier.call();

			if (connection == null) {
				throw new AssertionError(
					"A JDBC connection supplier returned a null connection"
				);
			}

			return connection;
		} catch (final Exception exc) {
			if (exc instanceof SQLException) {
				throw (SQLException) exc;
			} else {
				throw new SQLException(exc);
			}
		}
	}

	/**
	 * Closes a JDBC connection, rolling back the last transaction if necessary.
	 * Unless a database access error is reported by the underlying driver, closing
	 * an already closed {@code Connection} is a no-op.
	 *
	 * @param connection The connection to close.
	 * @throws SQLException If some SQL error occurs during the operation.
	 */
	private static void closeConnection(final Connection connection) throws SQLException {
		SQLException throwedException = null;

		try {
			if (!connection.isClosed() && !connection.getAutoCommit()) {
				connection.rollback();
			}
		} catch (final SQLException exc) {
			throwedException = exc;
		} finally {
			// Close the connection no matter if we succeeded doing the rollback
			try {
				connection.close();
			} catch (final SQLException exc) {
				throwedException = exc;
			}
		}

		if (throwedException != null) {
			throw throwedException;
		}
	}
}
