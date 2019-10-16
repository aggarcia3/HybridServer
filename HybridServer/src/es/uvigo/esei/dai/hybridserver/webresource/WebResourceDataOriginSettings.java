package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Specifies the contract that every data origin settings provider should follow
 * for it to work with the already existing classes.
 *
 * @author Alejandro González García
 */
interface WebResourceDataOriginSettings {
	/**
	 * Retrieves an instance of the web resource data origin factory that should be
	 * used to create the data origin this configuration is relevant to. For
	 * example, if the data origin settings are relevant for a file-based data
	 * origin, this method should return a factory of an appropriate file-based data
	 * origin.
	 *
	 * @return The described web resource data origin factory.
	 * @throws IllegalArgumentException If some property of the settings doesn't
	 *                                  allow to create a factory with that
	 *                                  settings. This is normally caused by bad
	 *                                  parameters passed to the factory
	 *                                  implementation constructor.
	 */
	public WebResourceDataOriginFactory getWebResourceDataOriginFactory();
}
