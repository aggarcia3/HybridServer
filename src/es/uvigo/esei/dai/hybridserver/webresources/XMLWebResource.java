package es.uvigo.esei.dai.hybridserver.webresources;

import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.MIME;

/**
 * Represents a XML document web resource.
 *
 * @author Alejandro González García
 */
public final class XMLWebResource extends WebResource<XMLWebResource> {
	private static final String TYPE_NAME = "xml";
	// Personally, I like text/xml more, as it conveys better the notion of XML
	// documents being readable by humans, but tests rule
	private static final String MIME_TYPE = MIME.APPLICATION_XML.getMime();

	static final XMLWebResource DUMMY = new XMLWebResource();

	/**
	 * Creates a placeholder XML web resource, satisfying the null object pattern.
	 * This constructor is not meant to be used directly by users of this class.
	 */
	public XMLWebResource() {
		this(UUID.nameUUIDFromBytes(new byte[0]), "");
	}

	/**
	 * Creates a new XML web resource from its UUID and content.
	 *
	 * @param uuid    The UUID of the XML document.
	 * @param content The content of the XML document.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	public XMLWebResource(final UUID uuid, final String content) {
		super(TYPE_NAME, MIME_TYPE, commonAttributesToMap(uuid, content, 2));
	}

	/**
	 * Creates a new XML web resource from its attributes.
	 *
	 * @param attributes The attributes of the XML web resource.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	private XMLWebResource(final Map<String, String> attributes) {
		super(TYPE_NAME, MIME_TYPE, attributes);
	}

	@Override
	protected XMLWebResource createFromAttributes(final Map<String, String> attributes) {
		return new XMLWebResource(attributes);
	}
}
