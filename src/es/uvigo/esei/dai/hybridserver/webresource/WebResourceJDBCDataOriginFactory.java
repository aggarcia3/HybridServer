package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.logging.Logger;

/**
 * A factory for a web resource data origin backed by a relational SQL DBMS,
 * which can be accessed using a JDBC driver.
 *
 * @author Alejandro González García
 */
final class WebResourceJDBCDataOriginFactory extends WebResourceDataOriginFactory {
	private final JDBCBackedWebResourceMap resourceMap;

	/**
	 * Creates a factory for a web resource data origin backed by a relational SQL
	 * DBMS.
	 *
	 * @param dbUrl        The JDBC URL to connect to the database. It is assumed
	 *                     that its driver is loaded, or is able to be loaded
	 *                     automatically.
	 * @param dbUser       The user name to use when logging in the database.
	 * @param dbPassword   The password to use when logging in the database.
	 * @param resourceType The type of web resource this map will serve.
	 * @param logger       The logger on which to log relevant information about the
	 *                     operation of the data origin.
	 * @throws IllegalArgumentException If any parameter is null.
	 */
	public WebResourceJDBCDataOriginFactory(final String dbUrl, final String dbUser, final String dbPassword, final WebResourceType resourceType, final Logger logger) {
		// Creating the map here is efficient, because the implementation lazily connects to the DB,
		// and it is a simple way to ensure that this factory will only create a single resource map ever.
		// It also reuses the parameter validation logic better
		this.resourceMap = new JDBCBackedWebResourceMap(dbUrl, dbUser, dbPassword, resourceType, logger);
	}

	@Override
	protected IOBackedWebResourceMap<String, WebResource> createResourceMap() {
		return resourceMap;
	}
}
