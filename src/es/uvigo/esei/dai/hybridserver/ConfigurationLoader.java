package es.uvigo.esei.dai.hybridserver;

import java.io.File;

/**
 * Defines the contract that any class capable of parsing and interpreting
 * configuration parameters should follow, so it essentially is a factory of
 * {@link Configuration} objects.
 *
 * @author Alejandro González García
 */
interface ConfigurationLoader<T> {
	public static final String CONFIGURATION_XSD_FILENAME = "configuration.xsd";

	/**
	 * Loads the configuration contained in a file.
	 *
	 * @param file The file to load configuration from.
	 * @return The initialized configuration object, according to the settings
	 *         contained in the file.
	 * @throws Exception If some exception occurred during parsing or reading.
	 */
	public Configuration load(final File file) throws Exception;

	/**
	 * Loads the configuration contained in a native format, specific to the loader.
	 * A native format is a format such that almost no conversion is needed to
	 * construct the resulting {@link Configuration} object. Implementation of this
	 * method is optional.
	 *
	 * @param input The native input format that contains the configuration
	 *              parameters.
	 * @return The initialized configuration object, according to the settings
	 *         contained in the input.
	 * @throws UnsupportedOperationException If the configuration loader doesn't
	 *                                       implement this operation.
	 * @throws Exception                     If some exception occurred during
	 *                                       parsing or reading.
	 * @see ConfigurationLoader#supportsNativeFormatLoading
	 */
	public default Configuration load(final T input) throws Exception {
		throw new UnsupportedOperationException(
			"This configuration loader doesn't support loading configuration from a native format"
		);
	}

	/**
	 * Returns whether this configuration loader supports the
	 * {@link ConfigurationLoader#load(T)} method. This method should be overridden
	 * when the associated method is overridden.
	 *
	 * @return True if this configuration loader supports loading configuration from
	 *         a native format, false otherwise.
	 */
	public default boolean supportsNativeFormatLoading() {
		return false;
	}
}
