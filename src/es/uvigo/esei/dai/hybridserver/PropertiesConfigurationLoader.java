package es.uvigo.esei.dai.hybridserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;

import static es.uvigo.esei.dai.hybridserver.ConfigurationLoaderConstants.NULL_INPUT;

/**
 * Loads a Hybrid Server configuration from a properties file. This
 * implementation of {@link ConfigurationLoader} supports loading configurations
 * from its native format, a {@link Properties} object.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
final class PropertiesConfigurationLoader implements ConfigurationLoader<Properties> {
	private static final String PORT = "port";
	private static final String NUM_CLIENTS = "numClients";
	private static final String DB_URL = "db.url";
	private static final String DB_USER = "db.user";
	private static final String DB_PASSWORD = "db.password";

	@Override
	public Configuration load(final File file) throws Exception {
		if (file == null) {
			throw new IllegalArgumentException(NULL_INPUT);
		}

		try (final Reader fileReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			final Properties properties = new Properties();

			properties.load(fileReader);

			return propertiesToConfiguration(properties);
		}
	}

	@Override
	public Configuration load(final Properties input) throws Exception {
		if (input == null) {
			throw new IllegalArgumentException(NULL_INPUT);
		}

		return propertiesToConfiguration(input);
	}

	@Override
	public boolean supportsNativeFormatLoading() {
		return true;
	}

	/**
	 * Converts a initialized {@link Properties} object to a {@code Configuration}
	 * object.
	 *
	 * @param properties The object to convert.
	 * @return The converted {@code Configuration} object.
	 * @throws IllegalArgumentException If some property in the {@link Properties}
	 *                                  object is invalid.
	 */
	private Configuration propertiesToConfiguration(final Properties properties) throws IllegalArgumentException {
		// The constructor does any validation needed
		return new Configuration(
			Integer.parseInt(properties.getProperty(PORT)),
			Integer.parseInt(properties.getProperty(NUM_CLIENTS)),
			null,
			properties.getProperty(DB_USER),
			properties.getProperty(DB_PASSWORD),
			properties.getProperty(DB_URL),
			Collections.emptyList()
		);
	}
}
