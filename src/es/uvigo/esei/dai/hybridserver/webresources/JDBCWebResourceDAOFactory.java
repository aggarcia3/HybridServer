package es.uvigo.esei.dai.hybridserver.webresources;

/**
 * A factory for a web resource DAO backed by a relational JDBC database.
 *
 * @author Alejandro González García
 */
public final class JDBCWebResourceDAOFactory implements WebResourceDAOFactory<JDBCWebResourceDAOSettings> {
	// Initialization-on-demand holder idiom
	private static final class JDBCWebResourceDAOFactoryInstanceHolder {
		static final JDBCWebResourceDAOFactory INSTANCE = new JDBCWebResourceDAOFactory();
	}

	private JDBCWebResourceDAOFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static JDBCWebResourceDAOFactory get() {
		return JDBCWebResourceDAOFactoryInstanceHolder.INSTANCE;
	}

	@Override
	public <T extends WebResource<T>> WebResourceDAO<T> createDAO(
		final JDBCWebResourceDAOSettings settings, final Class<T> webResourceType
	) {
		if (webResourceType == null) {
			throw new IllegalArgumentException("A JDBC-backed web resource DAO can't be instantiated for a null web resource type");
		}

		T dummyWebResource;
		try {
			dummyWebResource = webResourceType.cast(webResourceType.getDeclaredField("DUMMY").get(null));
		} catch (final ClassCastException | NoSuchFieldException | NullPointerException | IllegalArgumentException | IllegalAccessException exc) {
			throw new AssertionError(
				"Couldn't get a dummy web resource for a JDBC-backed web resource DAO. Does the web resource satisfy the expected contract?",
				exc
			);
		}

		return new JDBCWebResourceDAO<>(settings, dummyWebResource);
	}
}
