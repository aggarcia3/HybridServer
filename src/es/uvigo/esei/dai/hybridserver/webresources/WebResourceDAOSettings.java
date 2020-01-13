package es.uvigo.esei.dai.hybridserver.webresources;

/**
 * Defines the interface of objects that store the settings needed to create a
 * web resource DAO, providing read-only access to them.
 *
 * @author Alejandro González García
 *
 * @param <T> The type of the class that implements this class.
 */
public interface WebResourceDAOSettings<T extends WebResourceDAOSettings<T>> {
	/**
	 * Returns a {@link WebResourceDAOFactory} to use for instantiating the DAO
	 * configured by this settings object.
	 *
	 * @return The described factory.
	 */
	public WebResourceDAOFactory<T> getFactory();
}
