package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.EnumMap;

import static es.uvigo.esei.dai.hybridserver.webresource.MemoryWebResourceDAOSettings.MemoryWebResourceDAOSettingsList;

/**
 * Stores the settings needed to create a memory-backed web resource DAO, which
 * currently are no settings.
 *
 * @author Alejandro González García
 */
public final class MemoryWebResourceDAOSettings extends WebResourceDAOSettings<MemoryWebResourceDAOSettings, MemoryWebResourceDAOSettingsList, Void> {
	/**
	 * Creates an object which stores the settings needed to create a memory-backed
	 * web resource DAO.
	 *
	 * @param webResourceType The type of web resources that the DAO will provide
	 *                        access to.
	 */
	public MemoryWebResourceDAOSettings() {
		super(new EnumMap<>(MemoryWebResourceDAOSettingsList.class));
	}

	@Override
	public MemoryWebResourceDAOFactory getFactory() {
		return MemoryWebResourceDAOFactory.get();
	}

	static enum MemoryWebResourceDAOSettingsList {
		// No configurable options for this DAO
	}
}
