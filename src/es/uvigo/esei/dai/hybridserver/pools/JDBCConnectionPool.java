package es.uvigo.esei.dai.hybridserver.pools;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class JDBCConnectionPool {
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

	public final Connection getConnectionForThread(final Supplier<Connection> connectionStrategy, final int timeoutSeconds) throws SQLException {
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
}
