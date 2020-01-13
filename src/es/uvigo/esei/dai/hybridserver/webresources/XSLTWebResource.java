package es.uvigo.esei.dai.hybridserver.webresources;

import java.util.Map;
import java.util.UUID;

import es.uvigo.esei.dai.hybridserver.http.MIME;

/**
 * Represents a XSLT document web resource.
 *
 * @author Alejandro González García
 */
public final class XSLTWebResource extends WebResource<XSLTWebResource> {
	public static final String XSD_ATTRIBUTE = "xsd";

	private static final String TYPE_NAME = "xslt";
	// https://www.w3.org/TR/2007/REC-xslt20-20070123/#media-type-registration:
	// "Existing XSLT 1.0 stylesheets are most often described using the
	// unregistered media type "text/xsl"."
	// But tests expect application/xml for XSLT stylesheets, so we deliberately
	// ignore de-facto standards
	private static final String MIME_TYPE = MIME.APPLICATION_XML.getMime();

	private static final String NULL_XSD_MESSAGE = "A XSLT web resource can't be associated to a invlid XSD UUID";

	static final XSLTWebResource DUMMY = new XSLTWebResource();

	/**
	 * Creates a placeholder XSLT web resource, satisfying the null object pattern.
	 * This constructor is not meant to be used directly by users of this class.
	 */
	public XSLTWebResource() {
		this(UUID.nameUUIDFromBytes(new byte[0]), "", UUID.nameUUIDFromBytes(new byte[0]));
	}

	/**
	 * Creates a new XSLT web resource from its UUID, content and associated XSD.
	 *
	 * @param uuid    The UUID of the XSLT document.
	 * @param content The content of the XSLT document.
	 * @param xsd     The UUID of the associated XSD.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	public XSLTWebResource(final UUID uuid, final String content, final UUID xsd) {
		super(TYPE_NAME, MIME_TYPE, addSpecificAttributesToMap(commonAttributesToMap(uuid, content, 3), xsd));
	}

	/**
	 * Creates a new XSLT web resource from its attributes.
	 *
	 * @param attributes The attributes of the XSLT web resource.
	 * @throws IllegalArgumentException If some attribute is invalid.
	 */
	private XSLTWebResource(final Map<String, String> attributes) {
		super(TYPE_NAME, MIME_TYPE, attributes);

		final String xsd = attributes.get(XSD_ATTRIBUTE);
		try {
			if (xsd != null) {
				UUID.fromString(xsd);
			} else {
				throw new IllegalArgumentException();
			}
		} catch (final IllegalArgumentException exc) {
			throw new IllegalArgumentException(NULL_XSD_MESSAGE);
		}
	}

	@Override
	protected XSLTWebResource createFromAttributes(final Map<String, String> attributes) {
		return new XSLTWebResource(attributes);
	}

	/**
	 * Helper method that adds a list attributes specific to this web resource to a
	 * map.
	 *
	 * @param map The map that contains the common attributes to all web resources.
	 * @param xsd The UUID of the associated XSD.
	 * @return The {@code map} argument.
	 */
	private static Map<String, String> addSpecificAttributesToMap(final Map<String, String> map, final UUID xsd) {
		if (xsd == null) {
			throw new IllegalArgumentException(NULL_XSD_MESSAGE);
		}

		map.put(XSD_ATTRIBUTE, xsd.toString());

		return map;
	}
}
