package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Describes the contract that every object capable of instantiating a
 * {@link WebResourceDAO} should follow.
 *
 * @author Alejandro González García
 *
 * @param <T> The specific settings class type for the particular implementation
 *            of this factory: different implementations instantiate different
 *            DAOs and need different settings.
 */
@FunctionalInterface
public interface WebResourceDAOFactory<T extends WebResourceDAOSettings<?, ?, ?>> {
	/**
	 * Returns a ready to use web resource DAO configured according to the provided
	 * DAO-specific settings.
	 *
	 * @param <U>             The type of web resources that will be stored to and
	 *                        recovered from the instantiated DAO.
	 *
	 * @param settings        The DAO-specific settings to use to configure the web
	 *                        resource DAO.
	 * @param webResourceType The type of web resources that the DAO will provide
	 *                        access to.
	 * @return The described web resource DAO. Implementations are free to return
	 *         existing DAO objects if they have the same configuration and visible
	 *         state.
	 * @throws IllegalArgumentException If a DAO can't be instantiated with the
	 *                                  provided arguments because they're invalid.
	 */
	public <U extends WebResource<U>> WebResourceDAO<U> createDAO(final T settings, final Class<U> webResourceType);
}
