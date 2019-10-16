package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.logging.Logger;

/**
 * Stores the settings for a web resource data origin backed by a relational SQL
 * DBMS, which can be accessed using a JDBC driver. This is useful to retrieve a
 * handle to that data source with {@link WebResourceDataOriginFactory}.
 *
 * @author Alejandro González García
 */
public final class WebResourceJDBCDataOriginSettings implements WebResourceDataOriginSettings {
	private final String dbUrl;
	private final String dbUser;
	private final String dbPassword;
	private final WebResourceType resourceType;
	private final Logger logger;

	/**
	 * Creates an object which stores the settings needed for a web resource data
	 * origin backed by a relational SQL DBMS. The parameters will be checked for
	 * correctness lazily, only when creating the actual means to access the data
	 * origin is needed, so being successful in constructing this object does not
	 * guarantee that getting actual access to the data origin will be successful
	 * too.
	 *
	 * @param dbUrl        The JDBC URL to connect to the database. It is assumed
	 *                     that its driver is loaded, or is able to be loaded
	 *                     automatically.
	 * @param dbUser       The user name to use when logging in the database.
	 * @param dbPassword   The password to use when logging in the database.
	 * @param resourceType The type of web resource this map will serve.
	 * @param logger       The logger on which to log relevant information about the
	 *                     operation of the data origin.
	 */
	public WebResourceJDBCDataOriginSettings(final String dbUrl, final String dbUser, final String dbPassword, final WebResourceType resourceType, final Logger logger) {
		this.dbUrl = dbUrl;
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.resourceType = resourceType;
		this.logger = logger;
	}

	@Override
	public WebResourceDataOriginFactory getWebResourceDataOriginFactory() {
		return new WebResourceJDBCDataOriginFactory(dbUrl, dbUser, dbPassword, resourceType, logger);
	}
}
