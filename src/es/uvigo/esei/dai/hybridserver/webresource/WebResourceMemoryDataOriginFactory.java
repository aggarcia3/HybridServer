package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * A factory for a web resource data origin backed by DRAM.
 *
 * @author Alejandro González García
 */
final class WebResourceMemoryDataOriginFactory extends WebResourceDataOriginFactory {
	// Initialization-on-demand holder idiom
	private static final class WebResourceMemoryDataOriginFactoryInstanceHolder {
		static final WebResourceMemoryDataOriginFactory INSTANCE = new WebResourceMemoryDataOriginFactory();
	}

	private WebResourceMemoryDataOriginFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static WebResourceMemoryDataOriginFactory get() {
		return WebResourceMemoryDataOriginFactoryInstanceHolder.INSTANCE;
	}

	@Override
	protected IOBackedWebResourceMap<String, WebResource> createResourceMap() {
		return new MemoryBackedWebResourceMap();
	}
}
