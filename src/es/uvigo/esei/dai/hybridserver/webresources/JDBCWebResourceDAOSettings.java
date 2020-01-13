package es.uvigo.esei.dai.hybridserver.webresources;

import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.pools.JDBCConnectionPool;

/**
 * Stores the settings needed to create a JDBC-backed web resource DAO.
 *
 * @author Alejandro González García
 */
public final class JDBCWebResourceDAOSettings implements WebResourceDAOSettings<JDBCWebResourceDAOSettings> {
	private final JDBCConnectionPool dbConnectionPool;
	private final Logger logger;

	/**
	 * Creates an object which stores the settings needed to create a JDBC-backed
	 * web resource DAO.
	 *
	 * @param dbConnectionPool The pool of JDBC connections to a relational
	 *                         database.
	 * @param logger           The logger to use for outputting information about
	 *                         the DAO operations.
	 * @throws IllegalArgumentException If any parameter is {@code null}, except
	 *                                  {@code logger}.
	 */
	public JDBCWebResourceDAOSettings(final JDBCConnectionPool dbConnectionPool, final Logger logger) {
		if (dbConnectionPool == null) {
			throw new IllegalArgumentException(
				"Can't associate a null JDBC connection pool to JDBC web resource DAO settings"
			);
		}

		this.logger = logger;
		this.dbConnectionPool = dbConnectionPool;
	}

	@Override
	public JDBCWebResourceDAOFactory getFactory() {
		return JDBCWebResourceDAOFactory.get();
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
	 * Returns the JDBC connection pool to use by the to-be-created DAO.
	 *
	 * @return The specified JDBC connection pool.
	 */
	public JDBCConnectionPool getJdbcConnectionPool() {
		return dbConnectionPool;
	}
}
