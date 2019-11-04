package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Stores the settings for a web resource DRAM-backed data origin, useful to retrieve
 * a handle to it with {@link WebResourceDataOriginFactory}.
 *
 * @author Alejandro González García
 */
public final class WebResourceMemoryDataOriginSettings implements WebResourceDataOriginSettings {
	@Override
	public WebResourceDataOriginFactory getWebResourceDataOriginFactory() {
		return WebResourceMemoryDataOriginFactory.get();
	}
}
