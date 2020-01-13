package es.uvigo.esei.dai.hybridserver.webresources;

/**
 * A factory for a web resource DAO backed by a P2P network and other DAO.
 *
 * @author Alejandro González García
 */
public final class WebResourceDAOP2PDecoratorFactory implements WebResourceDAOFactory<WebResourceDAOP2PDecoratorSettings> {
	// Initialization-on-demand holder idiom
	private static final class WebResourceDAOP2PDecoratorFactoryInstanceHolder {
		static final WebResourceDAOP2PDecoratorFactory INSTANCE = new WebResourceDAOP2PDecoratorFactory();
	}

	private WebResourceDAOP2PDecoratorFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static WebResourceDAOP2PDecoratorFactory get() {
		return WebResourceDAOP2PDecoratorFactoryInstanceHolder.INSTANCE;
	}

	@Override
	public <U extends WebResource<U>> WebResourceDAO<U> createDAO(final WebResourceDAOP2PDecoratorSettings settings, final Class<U> webResourceType) {
		settings.setWebResourceType(webResourceType);
		return new WebResourceDAOP2PDecorator<>(settings);
	}
}
