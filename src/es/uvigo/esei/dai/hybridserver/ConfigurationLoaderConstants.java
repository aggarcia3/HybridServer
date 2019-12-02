package es.uvigo.esei.dai.hybridserver;

/**
 * Contains constants that improve the consistency between configuration loader
 * implementations, without adding code repetition.
 *
 * @author Alejandro González García
 */
final class ConfigurationLoaderConstants {
	static final String NULL_INPUT = "A configuration can't be loaded from a null input";

	/**
	 * Forbids the instantiation of this class. Does nothing.
	 */
	private ConfigurationLoaderConstants() {}
}
