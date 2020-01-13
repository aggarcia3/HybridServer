package es.uvigo.esei.dai.hybridserver.webresources;

/**
 * Stores the settings needed to create a memory-backed web resource DAO, which
 * currently are no settings.
 *
 * @author Alejandro González García
 */
public final class MemoryWebResourceDAOSettings implements WebResourceDAOSettings<MemoryWebResourceDAOSettings> {
	@Override
	public MemoryWebResourceDAOFactory getFactory() {
		return MemoryWebResourceDAOFactory.get();
	}
}
