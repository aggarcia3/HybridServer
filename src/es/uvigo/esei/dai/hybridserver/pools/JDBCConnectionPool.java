package es.uvigo.esei.dai.hybridserver.pools;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class JDBCConnectionPool implements Closeable {
	private final Map<Integer, Connection> dbConnections = new ConcurrentHashMap<>();
	private final ThreadLocal<Integer> currentThreadId;
	private final AtomicInteger nextThreadId;

	// Initialization-on-demand holder idiom
	private static final class JDBCConnectionPoolInstanceHolder {
		static final JDBCConnectionPool INSTANCE = new JDBCConnectionPool();
	}

	private JDBCConnectionPool() {
		this.nextThreadId = new AtomicInteger();

		this.currentThreadId = ThreadLocal.withInitial(() -> {
			return nextThreadId.getAndIncrement();
		});
	}

	/**
	 * Gets the only instance in the JVM of this pool.
	 *
	 * @return The instance.
	 */
	public static JDBCConnectionPool get() {
		return JDBCConnectionPoolInstanceHolder.INSTANCE;
	}

	public final Connection getConnectionForCurrentThread(final Supplier<Connection> connectionStrategy, final int timeoutSeconds) throws SQLException {
		final int currentThreadId = this.currentThreadId.get();

		if (connectionStrategy == null) {
			throw new IllegalArgumentException("The connection strategy can't be null");
		}

		Connection currentThreadConnection = dbConnections.get(currentThreadId);
		if (currentThreadConnection == null || !currentThreadConnection.isValid(timeoutSeconds)) {
			if (currentThreadConnection != null) {
				// Actively try to close invalid (but once established) connections,
				// to maybe allow the JDBC driver to clean up its internal state
				try {
					currentThreadConnection.close();
				} catch (final SQLException ignore) {}
			}

			currentThreadConnection = connectionStrategy.get();
			if (currentThreadConnection == null) {
				throw new SQLException("Couldn't get a SQL connection");
			}

			dbConnections.put(currentThreadId, currentThreadConnection);
		}

		return currentThreadConnection;
	}

	public final void closeForCurrentThread() throws SQLException {
		final int currentThreadId = this.currentThreadId.get();
		final Connection threadConnection = dbConnections.get(currentThreadId);

		if (threadConnection != null) {
			// Bail out if the connection is closed. If an exception occurs, assume
			// it is not closed
			try {
				if (threadConnection.isClosed()) {
					return;
				}
			} catch (final SQLException ignored) {}

			threadConnection.close();
			// If we didn't close it successfully, it won't be removed from the map
			dbConnections.remove(currentThreadId);
		}
	}

	@Override
	public final void close() throws IOException {
		SQLException firstException = null;

		final Iterator<Connection> iter = dbConnections.values().iterator();
		while (iter.hasNext()) {
			final Connection connection = iter.next();

			try {
				closeConnection(connection);
				iter.remove(); // Removes the entry on the map
			} catch (final SQLException exc) {
				if (firstException != null) {
					firstException.setNextException(exc);
				} else {
					firstException = exc;
				}
			}
		}

		// Delay throwing exceptions until a best effort to close all the connections
		// has been made
		if (firstException != null) {
			throw new IOException(firstException);
		}
	}

	private final void closeConnection(final Connection connection) throws SQLException {
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
