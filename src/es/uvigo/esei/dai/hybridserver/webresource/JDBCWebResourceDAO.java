package es.uvigo.esei.dai.hybridserver.webresource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import es.uvigo.esei.dai.hybridserver.webresource.JDBCWebResourceDAOSettings.JDBCWebResourceDAOSettingsList;

import static es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAOConstants.INVALID_RESOURCE;
import static es.uvigo.esei.dai.hybridserver.webresource.WebResourceDAOConstants.WEB_RESOURCE_ALREADY_MAPPED;

/**
 * Models a JDBC web resource DAO, which maintains rows of web resources
 * the server has available for clients, identified by their UUID, in a database
 * table. This implementation of {@code WebResourceDataSource} encapsulates
 * {@code SQLException}s in {@code IOException}s. Each thread that uses this
 * DAO maintains a connection to the DB for itself, in order to maximize
 * performance and parallelism. Therefore, it is necessary to call its
 * {@link JDBCWebResourceDAO#close} method when the DAO is
 * not used anymore to close all the opened connections. Connections to the DBMS
 * will be lazily created as needed.
 *
 * @param <T> The type of the web resources contained in the DAO.
 *
 * @author Alejandro González García
 */
final class JDBCWebResourceDAO<T extends WebResource<T>> implements WebResourceDAO<T> {
	private final JDBCWebResourceDAOSettings settings;

	private final Set<String> attributeSet;
	private final String tableName;
	private final String tableAttributes;
	private final String sqlAttributeParameters;
	private final T dummyWebResource;

	private final Map<Integer, Connection> dbConnections = new ConcurrentHashMap<>();
	private final ThreadLocal<Integer> currentThreadId;
	private final AtomicInteger nextThreadId;

	/**
	 * Creates a JDBC backed web resource DAO, which will store and read web
	 * resources from a relational database.
	 *
	 * @param settings         The settings for this DAO, used to configure the
	 *                         connection to the DB.
	 * @param dummyWebResource The dummy web resource that will be used when access
	 *                         to web resource type-specific operations is needed,
	 *                         but no suitable web resource object is available.
	 * @throws IllegalArgumentException If {@code settings} is {@code null}.
	 */
	public JDBCWebResourceDAO(final JDBCWebResourceDAOSettings settings, final T dummyWebResource) {
		if (settings == null || dummyWebResource == null) {
			throw new IllegalArgumentException(
				"Can't create a JDBC-backed web resource DAO with null settings or a null dummy web resource"
			);
		}

		this.settings = settings;

		DriverManager.setLoginTimeout(settings.getDbLoginTimeout());

		this.attributeSet = dummyWebResource.getAttributeNames();

		final StringBuilder attributeStringBuilder = new StringBuilder();
		final StringBuilder sqlAttributeParametersBuilder = new StringBuilder();
		final Pattern forbiddenCharsPattern = Pattern.compile("[^0-9a-zA-Z_-]");
		for (final String attribute : attributeSet) {
			attributeStringBuilder.append(
				forbiddenCharsPattern.matcher(attribute).replaceAll("") // Just in case we fail to follow our own contract, to make internal SQL injection harder
			).append(", ");
			sqlAttributeParametersBuilder.append("?").append(", ");
		}

		// Delete last ", " if present
		final int attributeStringLength = attributeStringBuilder.length();
		if (attributeStringLength >= 2) {
			attributeStringBuilder.setLength(attributeStringLength - 2);
		}
		final int sqlAttributeParametersStringLength = sqlAttributeParametersBuilder.length();
		if (sqlAttributeParametersStringLength >= 2) {
			sqlAttributeParametersBuilder.setLength(sqlAttributeParametersStringLength - 2);
		}

		this.tableAttributes = attributeStringBuilder.toString();
		this.sqlAttributeParameters = sqlAttributeParametersBuilder.toString();
		this.dummyWebResource = dummyWebResource;
		this.tableName = dummyWebResource.getTypeName();

		// Initialize identifiers for threads
		this.nextThreadId = new AtomicInteger();
		this.currentThreadId = ThreadLocal.withInitial(() -> {
			return nextThreadId.getAndIncrement();
		});
	}

	@Override
	public T get(final UUID uuid) throws IOException {
		T webResource = null;

		if (uuid != null) {
			try {
				// Try to get a connection to the DB if needed
				final Connection dbConnection = connectToDbIfDeadOrGet();

				try (final PreparedStatement statement = dbConnection.prepareStatement(
					"SELECT " + tableAttributes + " FROM " + tableName + " WHERE LOWER(uuid) = LOWER(?) LIMIT 1;")
				) {
					statement.setString(1, uuid.toString());

					try (final ResultSet result = statement.executeQuery()) {
						// Get the row count from the first column of the first row of the result,
						// making sure it was not null
						if (result.next()) {
							webResource = webResourceFromResult(result);
						}
					}
				}
			} catch (final SQLException exc) {
				throw handleSQLException(exc);
			}
		}

		return webResource;
	}

	@Override
	public void put(final T webResource) throws IOException {
		// Check arguments for sanity
		if (webResource == null) {
			throw new IllegalArgumentException(INVALID_RESOURCE);
		}

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			// FIXME: the SQL-92 standard doesn't allow for ignoring already existing rows,
			// and fixes for that require either DBMS-specific extensions or executing more
			// than one statement. A more recent revision of the SQL standard allows it, but
			// most DBMS already have trouble following the SQL-92 standard, and the situation
			// is worse for the newer one.
			// As I think that the correction points to avoid doing more queries than necessary,
			// "Cuando se crea una página no debe hacerse una consulta de recuperación de información.",
			// I opted to used MySQL-specific extensions. Obviously, they won't work on other
			// kind of DBMS, and this DAO is meant to be as generic as possible, so that shouldn't
			// happen.
			try (final PreparedStatement statement = dbConnection.prepareStatement(
				"INSERT IGNORE INTO " + tableName + " (" + tableAttributes + ") VALUES (" + sqlAttributeParameters + ");")
			) {
				int i = 0;
				for (final String attribute : attributeSet) {
					statement.setString(++i, webResource.getAttribute(attribute));
				}

				if (statement.executeUpdate() < 1) {
					throw new IllegalStateException(WEB_RESOURCE_ALREADY_MAPPED);
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}
	}

	@Override
	public boolean remove(final UUID uuid) throws IOException {
		boolean resourceRemoved = false;

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final PreparedStatement statement = dbConnection.prepareStatement(
				"DELETE FROM " + tableName + " WHERE uuid = ?;")
			) {
				statement.setString(1, uuid.toString());

				resourceRemoved = statement.executeUpdate() > 0;
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}

		return resourceRemoved;
	}

	@Override
	public Set<UUID> uuidSet() throws IOException {
		final Set<UUID> uuidSet = new HashSet<>();

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery(
					"SELECT DISTINCT uuid FROM " + tableName + ";")
				) {
					// Populate the result set with all the UUIDs.
					// We ignore invalid UUID, as they can't be used as keys
					while (result.next()) {
						final String key = result.getString("uuid");
						try {
							if (key == null) {
								throw new IllegalArgumentException();
							}

							uuidSet.add(UUID.fromString(key));
						} catch (final IllegalArgumentException skip) {}
					}
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}

		return uuidSet;
	}

	@Override
	public Collection<T> webResources() throws IOException {
		final List<T> resourcesList = new ArrayList<>(); // ArrayList has much better locality of reference, and will be faster for small datasets 

		try {
			// Try to get a connection to the DB if needed
			final Connection dbConnection = connectToDbIfDeadOrGet();

			try (final Statement statement = dbConnection.createStatement()) {
				try (final ResultSet result = statement.executeQuery(
					"SELECT " + tableAttributes + " FROM " + tableName + ";")
				) {
					while (result.next()) {
						resourcesList.add(webResourceFromResult(result));
					}
				}
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		}

		return resourcesList;
	}

	/**
	 * @implNote The map might be inconsistently closed after this method returns if
	 *           other threads are using it in the meantime. To avoid this, an
	 *           external synchronization mechanism is needed.
	 */
	@Override
	public void close() throws IOException {
		SQLException firstException = null;

		for (final Connection dbConnection : dbConnections.values()) {
			try {
				if (!dbConnection.isClosed() && !dbConnection.getAutoCommit()) { // We use auto commit now, but that could change again...
					dbConnection.rollback();
				}
			} catch (final SQLException exc) {
				if (firstException != null) {
					firstException.setNextException(exc);
				} else {
					firstException = exc;
				}
			} finally {
				// Close the connection no matter if we succeeded doing the rollback
				try {
					dbConnection.close();
				} catch (final SQLException exc) {
					if (firstException != null) {
						firstException.setNextException(exc);
					} else {
						firstException = exc;
					}
				}
			}
		}

		// Delay throwing exceptions until a best effort to close all the connections
		// has been made
		if (firstException != null) {
			throw new IOException(firstException);
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

		if (currentThreadDbConnection == null || !currentThreadDbConnection.isValid(settings.getDbLoginTimeout())) {
			final Logger logger = settings.getLogger();

			if (logger != null) {
				logger.log(Level.FINE,
					"The thread with ID {0} is using a JDBC backed web resource DAO without a established DBMS connection. Trying to establish a connection...",
					currentThreadId.get()
				);
			}

			// Actively try to close invalid (but once established) connections,
			// to maybe allow the JDBC driver to clean up its internal state
			if (currentThreadDbConnection != null) {
				try {
					currentThreadDbConnection.close();
				} catch (final SQLException ignore) {}
			}

			final String dbUrl = settings.getValue(JDBCWebResourceDAOSettingsList.DB_URL);
			currentThreadDbConnection = DriverManager.getConnection(
				dbUrl,
				settings.getValue(JDBCWebResourceDAOSettingsList.DB_USER),
				settings.getValue(JDBCWebResourceDAOSettingsList.DB_PASSWORD)
			);

			// Register our new connection in the connection map, so it can be closed
			// when the server stops (i.e. the close method is invoked) and we can retrieve
			// it later
			dbConnections.put(currentThreadId.get(), currentThreadDbConnection);

			if (logger != null) {
				logger.log(Level.FINE, "Connection to {0} established successfully", dbUrl);
			}
		}

		return currentThreadDbConnection;
	}

	/**
	 * Handles a SQL exception, encapsulating it in an {@code IOException}, and
	 * logging a warning message to the logger associated to this DAO.
	 *
	 * @param exc The SQL exception to handle.
	 * @return The {@code SQLException} encapsulated in a {@code IOException}.
	 */
	private IOException handleSQLException(final SQLException exc) {
		final Logger logger = settings.getLogger();

		if (logger != null) {
			logger.log(Level.WARNING, "An error occured while doing an operation in a JDBC backed web resource DAO", exc);
		}

		return new IOException(exc);
	}

	/**
	 * Creates a web resource from the attributes contained in a result set, if
	 * possible. It is assumed that the result set is already positioned in the
	 * desired row.
	 *
	 * @param result The result set where the attributes to instantiate the web
	 *               resource with are.
	 * @return The aforementioned web resource.
	 * @throws SQLException If some attribute of the web resource is missing from
	 *                      the result set, has an invalid value, or a DB access
	 *                      error occurred.
	 */
	private T webResourceFromResult(final ResultSet result) throws SQLException {
		final Map<String, String> attributeValuesMap = new HashMap<>(
			(int) Math.ceil(attributeSet.size() / 0.75)
		);

		for (final String attribute : attributeSet) {
			attributeValuesMap.put(attribute, result.getString(attribute));
		}

		try {
			return dummyWebResource.createFromAttributes(attributeValuesMap);
		} catch (final IllegalArgumentException exc) {
			throw new SQLException(exc);
		}
	}
}
