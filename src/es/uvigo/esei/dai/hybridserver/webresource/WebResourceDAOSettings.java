package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stores the settings needed to create a web resource DAO, providing read-only
 * access to them.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of the class that implements this class.
 * @param <K> The enumeration of the possible settings for a particular
 *            {@code WebResourceDAOSettings} subclass.
 * @param <V> The type of the values that the settings can take for a particular
 *            {@code WebResourceDAOSettings} subclass.
 */
public abstract class WebResourceDAOSettings<T extends WebResourceDAOSettings<?, ?, ?>, K extends Enum<K>, V> {
	// This map is unnecessary with the current implementations of this abstract class.
	// However, in the past, it was used by the JDBC one. I leave it here because it doesn't hurt,
	// and I might use it again before release
	private final Map<K, V> settingsMap;

	/**
	 * Creates a new object which stores the settings needed to create a web
	 * resource DAO, from a map that contains pairs of settings with their value.
	 * This constructor doesn't perform any check on the value of the settings.
	 *
	 * @param settingsMap     The described map.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	protected WebResourceDAOSettings(final EnumMap<K, V> settingsMap) {
		if (settingsMap == null) {
			throw new IllegalArgumentException("A web resource DAO can't be associated a null settings map");
		}

		this.settingsMap = Collections.unmodifiableMap(settingsMap);
	}

	/**
	 * Returns the value that has been associated to a web resource DAO setting.
	 *
	 * @param setting The setting to retrieve its value of.
	 * @return The value of the setting. If no value was associated to the setting,
	 *         then the return value is {@code null}. Specific implementations of
	 *         this class may forbid null values, and if so then invoking
	 *         {@link containsSetting} to determine whether the setting was set to
	 *         null is not necessary.
	 */
	public final V getValue(final K setting) {
		return settingsMap.get(setting);
	}

	/**
	 * Checks whether the specified web resource DAO setting was assigned a value.
	 * Specific implementations of this class may forbid null values.
	 *
	 * @param setting The setting to check.
	 * @return True if the web resource DAO setting was assigned a value, false
	 *         otherwise.
	 */
	public final boolean hasSetting(final K setting) {
		return settingsMap.containsKey(setting);
	}

	/**
	 * Returns a {@link WebResourceDAOFactory} to use for instantiating the DAO
	 * configured by this settings object.
	 *
	 * @return The described factory.
	 */
	public abstract WebResourceDAOFactory<T> getFactory();
}
