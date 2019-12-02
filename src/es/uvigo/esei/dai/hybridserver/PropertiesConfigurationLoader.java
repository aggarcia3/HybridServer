package es.uvigo.esei.dai.hybridserver;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Predicate;

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

	private static final Map<String, Predicate<String>> CONFIGURATION_PARAMETERS_PREDICATES;

	static {
		final Map<String, Predicate<String>> parametersPredicates = new HashMap<>((int) Math.ceil(5 / 0.75));

		// Predicate that every accepted natural integer configuration parameter
		// must validate
		final Predicate<String> naturalIntegerPredicate = (final String value) -> {
			try {
				int intValue = Integer.parseInt(value);

				// Only positive integers greater than 0 are valid
				if (intValue < 1) {
					throw new NumberFormatException();
				}

				return true;
			} catch (final NumberFormatException exc) {
				return false;
			}
		};

		// Predicate that every non-null parameter must validate
		final Predicate<String> nonNullStringPredicate = (final String value) -> {
			return value != null;
		};

		// Actual configuration parameters, with their default values and
		// validation predicates
		parametersPredicates.put(PORT, naturalIntegerPredicate);
		parametersPredicates.put(NUM_CLIENTS, naturalIntegerPredicate);
		parametersPredicates.put(DB_URL, nonNullStringPredicate);
		parametersPredicates.put(DB_USER, nonNullStringPredicate);
		parametersPredicates.put(DB_PASSWORD, nonNullStringPredicate);

		// Wrap the map as an unmodifiable one to better state our vision
		// of it being read-only after initialization
		CONFIGURATION_PARAMETERS_PREDICATES = Collections.unmodifiableMap(parametersPredicates);
	}

	@Override
	public Configuration load(final File file) throws Exception {
		if (file == null) {
			throw new IllegalArgumentException(NULL_INPUT);
		}

		try (final Reader fileReader = new FileReader(file)) {
			final Properties properties = new Properties();
			properties.load(new FileReader(file, StandardCharsets.UTF_8));

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
		// Validate parameters
		for (final Entry<String, Predicate<String>> configurationEntry : CONFIGURATION_PARAMETERS_PREDICATES.entrySet()) {
			final String key = configurationEntry.getKey();
			final String readValue = properties.getProperty(key);

			if (!configurationEntry.getValue().test(readValue)) {
				throw new IllegalArgumentException(
					"The configuration parameter \"" + key + "\" doesn't follow the expected format"
				);
			}
		}

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
