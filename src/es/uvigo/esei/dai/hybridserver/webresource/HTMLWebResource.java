package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a HTML document web resource.
 *
 * @author Alejandro González García
 */
public final class HTMLWebResource extends WebResource<HTMLWebResource> {
	static final HTMLWebResource DUMMY = new HTMLWebResource(
		UUID.nameUUIDFromBytes(new byte[0]), ""
	);

	private static final String TYPE_NAME = "html";
	private static final String MIME_TYPE = "text/html";

	/**
	 * Creates a new HTML web resource from its UUID and content.
	 *
	 * @param uuid    The UUID of the HTML document.
	 * @param content The content of the HTML document.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	public HTMLWebResource(final UUID uuid, final String content) {
		super(TYPE_NAME, MIME_TYPE, commonAttributesToMap(uuid, content, 2));
	}

	/**
	 * Creates a new HTML web resource from its attributes.
	 *
	 * @param attributes The attributes of the HTML web resource.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	private HTMLWebResource(final Map<String, String> attributes) {
		super(TYPE_NAME, MIME_TYPE, attributes);
	}

	@Override
	protected HTMLWebResource createFromAttributes(final Map<String, String> attributes) {
		return new HTMLWebResource(attributes);
	}
}
