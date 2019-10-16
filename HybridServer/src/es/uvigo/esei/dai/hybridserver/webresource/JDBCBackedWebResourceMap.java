package es.uvigo.esei.dai.hybridserver.webresource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Models a JDBC web resource map, which maintains a map of web resources the
 * server has available for clients, identified by their UUID, in a database.
 * This implementation of {@code IOBackedMap} encapsulates {@code SQLException}s
 * in {@code IOException}s. Each thread that uses this map maintains a
 * connection to the DB for itself, in order to maximize performance and
 * parallelism. Therefore, it is necessary to call its
 * {@link JDBCBackedWebResourceMap#close} method when the map is not used anymore
 * to close all the opened connections. Connections to the DBMS will be lazily
 * created as needed.
 *
 * @author Alejandro González García
 * @implNote The implementation of the non-default methods of the interface is
 *           atomic and thread-safe. No guarantees are made for the methods
 *           which have a default implementation in the interface, which is left
 *           as-is.
 */
final class JDBCBackedWebResourceMap implements IOBackedWebResourceMap<String, WebResource> {
	private static final int LOGIN_TIMEOUT = 30;

	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;
	private final WebResourceType resourceType;
	private final Logger logger;

	private final Map<Integer, Connection> dbConnections = new ConcurrentHashMap<>();
	private final ThreadLocal<Integer> currentThreadId;
	private final AtomicInteger nextThreadId;

	/**
	 * Creates a JDBC backed resource map, which will store and read web resources
	 * from a relational database.
	 *
	 * @param dbUrl        The JDBC URL to connect to the database. It is assumed
	 *                     that its driver is loaded, or is able to be loaded
	 *                     automatically.
	 * @param dbUser       The user name to use when logging in the database.
	 * @param dbPassword   The password to use when logging in the database.
	 * @param resourceType The type of web resource this map will serve.
	 * @param logger       The logger on which to log relevant information about the
	 *                     operation of the object.
	 * @throws IllegalArgumentException If any parameter is null.
	 */
	public JDBCBackedWebResourceMap(final String dbUrl, final String dbUser, final String dbPassword, final WebResourceType resourceType, final Logger logger) {
		if (dbUrl == null || dbUser == null || dbPassword == null || resourceType == null || logger == null) {
			throw new IllegalArgumentException("A piece of information provided to create a JDBC resource map was null");
		}

		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.resourceType = resourceType;
		this.logger = logger;

		// Wait a maximum of LOGIN_TIMEOUT seconds for logging in the DBMS
		DriverManager.setLoginTimeout(LOGIN_TIMEOUT);

		// Initialize identifiers for threads
		this.nextThreadId = new AtomicInteger();
		this.currentThreadId = ThreadLocal.withInitial(() -> {
			return nextThreadId.getAndIncrement();
		});
	}

	@Override
	public int size() throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + resourceType.getDbTable() + ";")) {
					int size = -1;

					// Get the row count from the first column of the first row of the result,
					// making sure it was not null
					if (result.next()) {
						size = result.getInt(1);
						if (result.wasNull()) {
							size = -1;
						}
					}

					if (size < 0) {
						throw new SQLException("Couldn't retrieve the number of web resources from the result set");
					}

					// End the current transaction
					dbConnection.commit();

					return size;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public boolean isEmpty() throws IOException {
		return size() == 0;
	}

	@Override
	public boolean containsKey(final String key) throws IOException {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(final WebResource value) throws IOException {
		// The interface doesn't allow null values
		try {
			validateValue(value);
		} catch (final IllegalArgumentException exc) {
			return false;
		}

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final PreparedStatement statement = dbConnection.prepareStatement("SELECT COUNT(*) FROM " + resourceType.getDbTable() + " WHERE content = ?;")) {
				statement.setString(1, value.getContent());

				try (final ResultSet result = statement.executeQuery()) {
					int size = -1;

					// Get the row count from the first column of the first row of the result,
					// making sure it was not null
					if (result.next()) {
						size = result.getInt(1);
						if (result.wasNull()) {
							size = -1;
						}
					}

					if (size < 0) {
						throw new SQLException("Couldn't retrieve the number of web resources from the result set");
					}

					// End the current transaction
					dbConnection.commit();

					return size > 0;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public WebResource get(final String key) throws IOException {
		// The interface doesn't allow non-UUID keys
		try {
			validateKey(key);
		} catch (final IllegalArgumentException exc) {
			return null;
		}

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final PreparedStatement statement = dbConnection.prepareStatement("SELECT content FROM " + resourceType.getDbTable() + " WHERE LOWER(uuid) = LOWER(?);")) {
				statement.setString(1, key);

				try (final ResultSet result = statement.executeQuery()) {
					WebResource webResource = null;

					// Get the row count from the first column of the first row of the result,
					// making sure it was not null
					if (result.next()) {
						final String content = result.getString("content");

						if (content == null) {
							throw new SQLException("Couldn't retrieve a non-null content from a UUID. Is the table schema well defined?");
						}

						webResource = new WebResource(content);
					}

					// End the current transaction
					dbConnection.commit();

					return webResource;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public WebResource put(final String key, final WebResource value) throws IOException {
		// Check arguments for sanity
		try {
			validateKey(key);
			validateValue(value);
		} catch (final IllegalArgumentException exc) {
			throw exc;
		}

		final String actualKey = key.toLowerCase();

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			// The put method contract mandates that we return the previous content
			// for the UUID
			String previousValue = null;

			try (final PreparedStatement selectPrevious = dbConnection.prepareStatement("SELECT content FROM " + resourceType.getDbTable() + " WHERE uuid = ?;")) {
				selectPrevious.setString(1, actualKey);

				try (final ResultSet previousRow = selectPrevious.executeQuery()) {
					if (previousRow.next()) {
						previousValue = previousRow.getString("content");
					}
				}
			}

			// Only do the actual data updating if necessary
			if (!value.getContent().equals(previousValue)) {
				try (final PreparedStatement insert = getPutInsertStatement()) {
					try (final PreparedStatement update = getPutUpdateStatement()) {
						doDBValuePut(previousValue, insert, update, actualKey, value.getContent(), false);
					}
				}
			}

			// End this transaction successfully
			dbConnection.commit();

			return previousValue != null ? new WebResource(previousValue) : null;
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public WebResource remove(final String key) throws IOException {
		// Check arguments for sanity
		try {
			validateKey(key);
		} catch (final IllegalArgumentException exc) {
			throw exc;
		}

		final String actualKey = key.toLowerCase();

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			// The remove method contract mandates that we return the previous content
			// for the UUID
			String previousValue = null;

			try (final PreparedStatement selectPrevious = dbConnection.prepareStatement("SELECT content FROM " + resourceType.getDbTable() + " WHERE uuid = ?;")) {
				selectPrevious.setString(1, actualKey);

				try (final ResultSet previousRow = selectPrevious.executeQuery()) {
					if (previousRow.next()) {
						previousValue = previousRow.getString("content");
					}
				}
			}

			// Only do the data remove DML statement if necessary
			if (previousValue != null) {
				try (final PreparedStatement delete = dbConnection.prepareStatement("DELETE FROM " + resourceType.getDbTable() + " WHERE uuid = ?;")) {
					delete.setString(1, actualKey);

					delete.executeUpdate();
				}
			}

			// End this transaction successfully
			dbConnection.commit();

			return previousValue != null ? new WebResource(previousValue) : null;
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends WebResource> m) throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			// Store the previous entries in a map to know their previous values,
			// so we can issue UPDATE or INSERT statements accordingly.
			// Standard SQL doesn't provide a "INSERT IF NOT EXISTS" statement
			// compatible with all RDBMS
			final Map<String, String> previousEntries = new HashMap<>();
			try (final Statement select = dbConnection.createStatement()) {
				select.executeQuery("SELECT uuid, content FROM " + resourceType.getDbTable() + ";");

				try (final ResultSet entries = select.getResultSet()) {
					while (entries.next()) {
						try {
							final String key = entries.getString("uuid");
							final String value = entries.getString("content");

							validateKey(key);
							validateValue(value);

							previousEntries.put(key.toLowerCase(), value);
						} catch (final IllegalArgumentException skip) {}
					}
				}
			}

			// Use batch updates if supported for performance
			final boolean useBatchUpdates = dbConnection.getMetaData().supportsBatchUpdates();

			// Precompile the statements we will use
			try (final PreparedStatement insert = getPutInsertStatement()) {
				try (final PreparedStatement update = getPutUpdateStatement()) {
					for (Map.Entry<? extends String, ? extends WebResource> entry : m.entrySet()) {
						final String uuid = entry.getKey();
						final WebResource webResource = entry.getValue();

						// Check that the key and value are okay
						try {
							validateKey(uuid);
							validateValue(webResource);
						} catch (final IllegalArgumentException exc) {
							throw exc;
						}

						// Put the row in the table
						doDBValuePut(previousEntries.get(uuid.toLowerCase()), insert, update, uuid.toLowerCase(), webResource.getContent(), useBatchUpdates);
					}

					// Execute the update batches if necessary
					if (useBatchUpdates) {
						insert.executeBatch();
						update.executeBatch();
					}

					// Everything went fine, so commit the insertions and/or updates
					dbConnection.commit();
				}
			}
		} catch (final IllegalArgumentException | SQLException exc) {
			// Undo any modifications done
			final Connection dbConnection = getCurrentThreadDbConnection();
			if (dbConnection != null) {
				try {
					dbConnection.rollback();
				} catch (final SQLException ignore) {}
			}

			logException(exc);

			// Rethrow the appropriate exception type
			if (exc instanceof SQLException) {
				throw new IOException(exc);
			} else {
				throw (IllegalArgumentException) exc;
			}
		}
	}

	@Override
	public void clear() throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				statement.executeUpdate("DELETE FROM " + resourceType.getDbTable() + ";");
			}

			// End this transaction successfully
			dbConnection.commit();
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public Set<String> keySet() throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery("SELECT DISTINCT uuid FROM " + resourceType.getDbTable() + ";")) {
					final Set<String> keySet = new HashSet<>();

					// Populate the result key set with all the UUID.
					// We ignore invalid UUID, as trying to use them as keys
					// will break assumptions, and normalize them to lower case
					while (result.next()) {
						final String key = result.getString("uuid");
						try {
							validateKey(key);
							keySet.add(key.toLowerCase());
						} catch (final IllegalArgumentException skip) {}
					}

					// End the current transaction
					dbConnection.commit();

					return keySet;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public Collection<WebResource> values() throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery("SELECT content FROM " + resourceType.getDbTable() + ";")) {
					final Collection<WebResource> values = new LinkedList<>();

					while (result.next()) {
						final String value = result.getString("content");
						if (value != null) {
							values.add(new WebResource(value));
						}
					}

					// End the current transaction
					dbConnection.commit();

					return values;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public Set<Map.Entry<String, WebResource>> entrySet() throws IOException {
		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery("SELECT DISTINCT uuid, content FROM " + resourceType.getDbTable() + ";")) {
					final Set<Map.Entry<String, WebResource>> entrySet = new HashSet<>();

					// Populate the result key set with all the UUID.
					// We ignore invalid UUID, as trying to use them as keys
					// will break assumptions, and normalize them to lower case
					while (result.next()) {
						final String key = result.getString("uuid");
						final String value = result.getString("content");
						try {
							validateKey(key);
							validateValue(value);

							entrySet.add(new AbstractMap.SimpleEntry<>(key, new WebResource(value)));
						} catch (final IllegalArgumentException skip) {}
					}

					// End the current transaction
					dbConnection.commit();

					return entrySet;
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	/**
	 * @implNote The map might be inconsistently closed after this method returns if
	 *           other threads are using it in the meantime. To avoid this, an
	 *           external synchronization mechanism is needed.
	 */
	@Override
	public void close() throws IOException {
		for (final Connection dbConnection : dbConnections.values()) {
			try {
				if (!dbConnection.isClosed() && !dbConnection.getAutoCommit()) {
					dbConnection.rollback();
				}
			} catch (final SQLException exc) {
				throw new IOException(exc);
			} finally {
				// Close the connection no matter if we succeeded doing the rollback
				try {
					dbConnection.close();
				} catch (final SQLException exc) {
					throw new IOException(exc);
				}
			}
		}
	}

	/**
	 * Does a best effort to get a connection to the database for the current
	 * thread, in case the connection with the DB was lost or it was not established
	 * yet.
	 *
	 * @return The new connection made with the DB if necessary, or a previously
	 *         established one if it is still valid.
	 * @throws SQLException If it was not possible to get a connection with the
	 *                      database after trying.
	 */
	private Connection connectToDbIfDeadOrGet() throws SQLException {
		Connection currentThreadDbConnection = dbConnections.get(currentThreadId.get());

		if (currentThreadDbConnection == null || !currentThreadDbConnection.isValid(LOGIN_TIMEOUT)) {
			logger.log(Level.INFO, "Using a JDBC backed resource map without a established DBMS connection. Trying to establish a connection...");

			// Actively try to close invalid (but once established) connections,
			// to maybe allow the JDBC driver to clean up its internal state
			if (currentThreadDbConnection != null) {
				try {
					currentThreadDbConnection.close();
				} catch (final SQLException ignore) {}
			}

			currentThreadDbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

			// We want to handle transactions ourselves, for better performance, isolation and atomicity
			currentThreadDbConnection.setAutoCommit(false);

			// Register our new connection in the connection map, so it can be closed
			// when the server stops (i.e. the close method is invoked) and we can retrieve
			// it later
			dbConnections.put(currentThreadId.get(), currentThreadDbConnection);

			logger.log(Level.INFO, "Connection established to {0} successfully", dbUrl);
		}

		return currentThreadDbConnection;
	}

	/**
	 * Retrieves the connection that this thread (and only this thread) will use to
	 * communicate with the DBMS.
	 *
	 * @return The described connection.
	 */
	private Connection getCurrentThreadDbConnection() {
		return dbConnections.get(currentThreadId.get());
	}

	/**
	 * Creates a new PreparedStatement for the current thread DB connection to
	 * insert a row as a part of a put operation. This method is to be used in
	 * conjunction with {@link JDBCBackedWebResourceMap#getPutUpdateStatement} and
	 * {@link JDBCBackedWebResourceMap#doDBValuePut}.
	 *
	 * @return The created PreparedStatement.
	 * @throws SQLException If DBMS or SQL error occurs during the creation.
	 */
	private PreparedStatement getPutInsertStatement() throws SQLException {
		// Like the "INSERT IF NOT EXISTS" most DBMS provide
		// in one way or another, but in standard SQL.
		// Based on https://stackoverflow.com/a/17067131
		return getCurrentThreadDbConnection().prepareStatement(
			"INSERT INTO " + resourceType.getDbTable() + " (uuid, content) VALUES (?, ?);"
		);
	}

	/**
	 * Creates a new PreparedStatement for the current thread DB connection to
	 * update existing rows as a part of a put operation. This method is to be used
	 * in conjunction with
	 * {@link JDBCBackedWebResourceMap#getPutInsertStatement} and
	 * {@link JDBCBackedWebResourceMap#doDBValuePut}.
	 *
	 * @return The created PreparedStatement.
	 * @throws SQLException If DBMS or SQL error occurs during the creation.
	 */
	private PreparedStatement getPutUpdateStatement() throws SQLException {
		return getCurrentThreadDbConnection().prepareStatement(
			"UPDATE " + resourceType.getDbTable() + " SET content = ? WHERE uuid = ?;"
		);
	}

	/**
	 * Performs a put operation in the database, or registers the updates as a part
	 * of a batch of updates. It is assumed that all the parameters are valid. The
	 * caller of this method is responsible of guaranteeing that transactions and
	 * exceptions are handled properly.
	 *
	 * @param previousValue The value previously associated with the key. It affects
	 *                      whether a INSERT or UPDATE statement will be issued.
	 * @param insert        The prepared statement to use for insertions, retrieved
	 *                      by
	 *                      {@link JDBCBackedWebResourceMap#getPutInsertStatement}.
	 * @param update        The prepared statement to use for updates, retrieved
	 *                      by
	 *                      {@link JDBCBackedWebResourceMap#getPutUpdateStatement}.
	 * @param actualKey     The key to put a value into.
	 * @param value         The value to put in the key.
	 * @param isBatch       True if and only if the update should be registered as a
	 *                      part of a batch of updates. Otherwise, if it is false,
	 *                      the update will be executed immediately.
	 * @throws SQLException If some DBMS or SQL error occurs during the operation.
	 */
	private void doDBValuePut(final String previousValue, final PreparedStatement insert, final PreparedStatement update, final String actualKey, final String value, final boolean isBatch) throws SQLException {
		PreparedStatement statementToUse;

		if (previousValue != null) {
			// Issue a UPDATE statement to change the existing value
			statementToUse = update;

			statementToUse.setString(1, value);
			statementToUse.setString(2, actualKey);
		} else {
			// Issue a INSERT statement to add a new value
			statementToUse = insert;

			statementToUse.setString(1, actualKey);
			statementToUse.setString(2, value);
		}

		if (isBatch) {
			statementToUse.addBatch();
		} else {
			statementToUse.executeUpdate();
		}
	}

	/**
	 * Handles a SQL exception, encapsulating it in an {@code IOException}, rolling
	 * back the current transaction, and logging a warning message to the logger
	 * associated to this map.
	 *
	 * @param exc The SQL exception to handle.
	 * @return The {@code SQLException} encapsulated in a {@code IOException}.
	 */
	private IOException handleSQLException(final SQLException exc) {
		final Connection dbConnection = getCurrentThreadDbConnection();
		if (dbConnection != null) {
			try {
				dbConnection.rollback();
			} catch (final SQLException ignore) {}
		}

		logException(exc);

		return new IOException(exc);
	}

	/**
	 * Logs a warning to the logger associated to this map, indicating an error
	 * condition caused by an exception.
	 *
	 * @param exc The exception that caused the error condition.
	 */
	private void logException(final Exception exc) {
		if (logger != null) {
			logger.log(Level.WARNING, "An error occured while doing an operation in a JDBC backed resource map", exc);
		}
	}

	/**
	 * Checks whether the key specified as a parameter is valid.
	 *
	 * @param key The key to check.
	 * @throws IllegalArgumentException If the key is not valid.
	 */
	private void validateKey(final String key) {
		UUID.fromString(key);
	}

	/**
	 * Checks whether the value, specified as a content string used internally by
	 * this map, is valid.
	 *
	 * @param value The value to check.
	 * @throws IllegalArgumentException If the value is not valid.
	 */
	private void validateValue(final String value) {
		if (value == null) {
			throw new IllegalArgumentException("IO-backed web resource maps don't support null values");
		}
	}

	/**
	 * Checks whether the value, specified as a {@link WebResource}, is valid.
	 *
	 * @param value The value to check.
	 * @throws IllegalArgumentException If the value is not valid.
	 */
	private void validateValue(final WebResource value) {
		if (value == null) {
			throw new IllegalArgumentException("IO-backed web resource maps don't support null values");
		}
	}
}
