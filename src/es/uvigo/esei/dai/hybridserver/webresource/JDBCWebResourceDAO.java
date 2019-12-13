package es.uvigo.esei.dai.hybridserver.webresource;

import java.io.IOException;
import java.sql.Connection;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import es.uvigo.esei.dai.hybridserver.pools.JDBCConnectionPool;

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

	private final JDBCConnectionPool dbConnectionPool;
	private final Set<String> attributeSet;
	private final String tableName;
	private final String tableAttributes;
	private final String sqlAttributeParameters;
	private final T dummyWebResource;

	/**
	 * Creates a JDBC backed web resource DAO, which will store and read web
	 * resources from a relational database.
	 *
	 * @param settings         The settings for this DAO, used to access the DB.
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
		this.dbConnectionPool = settings.getJdbcConnectionPool();

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
	}

	@Override
	public T get(final UUID uuid) throws IOException {
		T webResource = null;
		Connection dbConnection = null;

		if (uuid != null) {
			try {
				// Try to get a connection to the DB if needed
				dbConnection = dbConnectionPool.take();

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
			} finally {
				dbConnectionPool.yield(dbConnection);
			}
		}

		return webResource;
	}

	@Override
	public void put(final T webResource) throws IOException {
		Connection dbConnection = null;

		// Check arguments for sanity
		if (webResource == null) {
			throw new IllegalArgumentException(INVALID_RESOURCE);
		}

		try {
			dbConnection = dbConnectionPool.take();

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
		} finally {
			dbConnectionPool.yield(dbConnection);
		}
	}

	@Override
	public boolean remove(final UUID uuid) throws IOException {
		boolean resourceRemoved = false;
		Connection dbConnection = null;

		try {
			dbConnection = dbConnectionPool.take();

			try (final PreparedStatement statement = dbConnection.prepareStatement(
				"DELETE FROM " + tableName + " WHERE uuid = ?;")
			) {
				statement.setString(1, uuid.toString());

				resourceRemoved = statement.executeUpdate() > 0;
			}
		} catch (final SQLException exc) {
			throw handleSQLException(exc);
		} finally {
			dbConnectionPool.yield(dbConnection);
		}

		return resourceRemoved;
	}

	@Override
	public Set<UUID> uuidSet() throws IOException {
		final Set<UUID> uuidSet = new HashSet<>();
		Connection dbConnection = null;

		try {
			dbConnection = dbConnectionPool.take();

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
		} finally {
			dbConnectionPool.yield(dbConnection);
		}

		return uuidSet;
	}

	@Override
	public Collection<T> webResources() throws IOException {
		final List<T> resourcesList = new ArrayList<>(); // ArrayList has much better locality of reference
		Connection dbConnection = null;

		try {
			dbConnection = dbConnectionPool.take();

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
		} finally {
			dbConnectionPool.yield(dbConnection);
		}

		return resourcesList;
	}

	/**
	 * @implNote This method closes the JDBC connection pool used by this DAO.
	 */
	@Override
	public void close() throws IOException {
		dbConnectionPool.close();
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
