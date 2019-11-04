package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Pure fabrication responsible of creating data origins (currently,
 * {@link IOBackedWebResourceMap}s) for types of web resources, taking as input
 * their specific configuration.
 *
 * @author Alejandro González García
 */
// This factory will provide a centralized place to instantiate a P2P data origin,
// which will be a decorator for another underlying data origin
public abstract class WebResourceDataOriginFactory {
	/**
	 * Creates a {@link IOBackedWebResourceMap} that will give access to a data
	 * source for web resources. The information used to decide the concrete data
	 * source and how it will be used is transparently determined by the
	 * {@link WebResourceDataOriginSettings} object that is received as a parameter.
	 *
	 * @param originSettings The settings of the concrete data source.
	 * @return The desired {@link IOBackedWebResourceMap}.
	 * @throws IllegalArgumentException If the {@code originSettings} parameter is
	 *                                  null, or some property of the
	 *                                  {@code originSettings} doesn't allow to
	 *                                  create the resource map.
	 */
	public static IOBackedWebResourceMap<String, WebResource> createWebResourceMap(final WebResourceDataOriginSettings originSettings) {
		if (originSettings == null) {
			throw new IllegalArgumentException("Can't create a web resource map without data origin settings");
		}

		return originSettings.getWebResourceDataOriginFactory().createResourceMap();
	}

	/**
	 * Performs the actual creation of a {@link IOBackedWebResourceMap} that will give
	 * access to the underlying data source for a type of web resource.
	 *
	 * @return The described {@link IOBackedWebResourceMap}.
	 */
	protected abstract IOBackedWebResourceMap<String, WebResource> createResourceMap();
}
