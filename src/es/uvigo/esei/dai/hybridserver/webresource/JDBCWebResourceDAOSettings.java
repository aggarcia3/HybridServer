package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.EnumMap;
import java.util.logging.Logger;

import es.uvigo.esei.dai.hybridserver.webresource.JDBCWebResourceDAOSettings.JDBCWebResourceDAOSettingsList;

/**
 * Stores the settings needed to create a JDBC-backed web resource DAO.
 *
 * @author Alejandro González García
 */
public final class JDBCWebResourceDAOSettings extends WebResourceDAOSettings<JDBCWebResourceDAOSettings, JDBCWebResourceDAOSettingsList, String> {
	private static final int DB_LOGIN_TIMEOUT = 30; // Seconds

	private final Logger logger;

	/**
	 * Creates an object which stores the settings needed to create a JDBC-backed
	 * web resource DAO.
	 *
	 * @param dbUrl      The URL the DBMS is listening on for incoming connections.
	 * @param dbUser     The user to log in as in the DBMS.
	 * @param dbPassword The password of the DBMS user.
	 * @param logger     The logger to use for outputting information about the DAO
	 *                   operations.
	 * @throws IllegalArgumentException If any parameter is {@code null}, except
	 *                                  {@code logger}.
	 */
	public JDBCWebResourceDAOSettings(
		final String dbUrl, final String dbUser, final String dbPassword, final Logger logger
	) {
		super(constructorArgsToMap(dbUrl, dbUser, dbPassword));

		this.logger = logger;
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
	 * Returns the maximum time the JDBC driver will wait for logging in the
	 * database, in seconds.
	 *
	 * @return The specified time.
	 */
	public int getDbLoginTimeout() {
		return DB_LOGIN_TIMEOUT;
	}

	/**
	 * Enumerates all the possible settings for this DAO.
	 *
	 * @author Alejandro González García
	 */
	static enum JDBCWebResourceDAOSettingsList {
		/**
		 * The URL the DBMS is listening on for incoming connections.
		 */
		DB_URL,
		/**
		 * The user to log in as in the DBMS.
		 */
		DB_USER,
		/**
		 * The password of the DBMS user.
		 */
		DB_PASSWORD
	}

	/**
	 * Converts a list of method parameters to a {@link EnumMap}, interpreting them as
	 * values, and deducing their corresponding setting by their position in the
	 * parameter list.
	 *
	 * @param dbUrl The URL the DBMS is listening on for incoming connections.
	 * @param dbUser The user to log in as in the DBMS.
	 * @param dbPassword The password of the DBMS user.
	 * @return The described map.
	 * @throws IllegalArgumentException If some parameter is {@code null}.
	 */
	private static EnumMap<JDBCWebResourceDAOSettingsList, String> constructorArgsToMap(
		final String dbUrl, final String dbUser, final String dbPassword
	) {
		EnumMap<JDBCWebResourceDAOSettingsList, String> toret;

		if (dbUrl == null || dbUser == null || dbPassword == null) {
			throw new IllegalArgumentException("A setting for a JDBC-backed web resource DAO can't be null");
		}

		toret = new EnumMap<>(JDBCWebResourceDAOSettingsList.class);
		toret.put(JDBCWebResourceDAOSettingsList.DB_URL, dbUrl);
		toret.put(JDBCWebResourceDAOSettingsList.DB_USER, dbUser);
		toret.put(JDBCWebResourceDAOSettingsList.DB_PASSWORD, dbPassword);

		assert toret.size() == JDBCWebResourceDAOSettingsList.values().length :
			"The number of settings passed to the map should equal the number of possible settings"
		;

		return toret;
	}
}
