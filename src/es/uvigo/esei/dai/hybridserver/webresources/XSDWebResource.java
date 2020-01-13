package es.uvigo.esei.dai.hybridserver.webresources;

import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.MIME;

/**
 * Represents a XSD document web resource.
 *
 * @author Alejandro González García
 */
public final class XSDWebResource extends WebResource<XSDWebResource> {
	private static final String TYPE_NAME = "xsd";
	private static final String MIME_TYPE = MIME.APPLICATION_XML.getMime();

	static final XSDWebResource DUMMY = new XSDWebResource();

	/**
	 * Creates a placeholder XSD web resource, satisfying the null object pattern.
	 * This constructor is not meant to be used directly by users of this class.
	 */
	public XSDWebResource() {
		this(UUID.nameUUIDFromBytes(new byte[0]), "");
	}

	/**
	 * Creates a new XSD web resource from its UUID and content.
	 *
	 * @param uuid    The UUID of the XSD document.
	 * @param content The content of the XSD document.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	public XSDWebResource(final UUID uuid, final String content) {
		super(TYPE_NAME, MIME_TYPE, commonAttributesToMap(uuid, content, 2));
	}

	/**
	 * Creates a new XSD web resource from its attributes.
	 *
	 * @param attributes The attributes of the XSD web resource.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	private XSDWebResource(final Map<String, String> attributes) {
		super(TYPE_NAME, MIME_TYPE, attributes);
	}

	@Override
	protected XSDWebResource createFromAttributes(final Map<String, String> attributes) {
		return new XSDWebResource(attributes);
	}
}
