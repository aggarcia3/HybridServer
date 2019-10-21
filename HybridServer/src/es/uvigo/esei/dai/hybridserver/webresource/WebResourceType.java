package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Represents a type of web resources that this server is able to serve.
 *
 * @author Alejandro González García
 */
// In the future, there will be a XML resource type
public enum WebResourceType {
	/**
	 * A HTML page.
	 */
	HTML("HTML");

	private String dbTable;

	/**
	 * Constructs a new enum constant. This method will be invoked only by the JVM
	 * during class initialization.
	 *
	 * @param dbTable The table on which data for this web resource type resides
	 *                  on a relational DBMS.
	 */
	private WebResourceType(final String dbTable) {
		if (dbTable == null) {
			throw new AssertionError();
		}

		this.dbTable = dbTable;
	}

	/**
	 * Returns the relational database table on which data for this web resource
	 * type resides.
	 *
	 * @return The described table name.
	 */
	final String getDbTable() {
		return dbTable;
	}
}
