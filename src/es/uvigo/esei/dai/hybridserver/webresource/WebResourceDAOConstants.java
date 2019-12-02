package es.uvigo.esei.dai.hybridserver.webresource;

/**
 * Contains constants that improve the consistency between web resource DAO
 * implementations, without adding code repetition.
 *
 * @author Alejandro González García
 */
final class WebResourceDAOConstants {
	static final String INVALID_RESOURCE = "A web resource DAO can't manipulate null web resources";
	static final String WEB_RESOURCE_ALREADY_MAPPED = "A web resource with that UUID already exists";

	/**
	 * Forbids the instantiation of this class. Does nothing.
	 */
	private WebResourceDAOConstants() {}
}
