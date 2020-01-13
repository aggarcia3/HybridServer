package es.uvigo.esei.dai.hybridserver.webresources;

/**
 * A factory for a web resource DAO backed by SDRAM.
 *
 * @author Alejandro González García
 */
public final class MemoryWebResourceDAOFactory implements WebResourceDAOFactory<MemoryWebResourceDAOSettings> {
	// Initialization-on-demand holder idiom
	private static final class MemoryWebResourceDAOFactoryInstanceHolder {
		static final MemoryWebResourceDAOFactory INSTANCE = new MemoryWebResourceDAOFactory();
	}

	private MemoryWebResourceDAOFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static MemoryWebResourceDAOFactory get() {
		return MemoryWebResourceDAOFactoryInstanceHolder.INSTANCE;
	}

	@Override
	public <T extends WebResource<T>> WebResourceDAO<T> createDAO(
		final MemoryWebResourceDAOSettings settings, final Class<T> webResourceType
	) {
		return new MemoryWebResourceDAO<>();
	}
}
