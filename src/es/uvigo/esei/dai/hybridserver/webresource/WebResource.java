package es.uvigo.esei.dai.hybridserver.webresource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Models a web resource, which has associated, immutable, type-specific
 * properties.
 * <p>
 * Implementations of this class are <b>required</b> to provide a
 * package-private static field called {@code DUMMY} (or public, if not in the
 * same package as this class), which contains a instance of that web resource
 * implementation, preferably with empty, interned attributes, following the
 * null object pattern where possible. Failure to obey that contract will result
 * in runtime exceptions.
 *
 * @param <T> The type of the subclass.
 *
 * @author Alejandro González García
 * @implNote The implementation of the class is thread-safe, provided references
 *           to this object are shared between threads with a happens-before
 *           memory relationship.
 */
public abstract class WebResource<T extends WebResource<T>> {
	public static final String UUID_ATTRIBUTE = "uuid";
	public static final String CONTENT_ATTRIBUTE = "content";

	private final String typeName;
	private final String mimeType;
	private final Map<String, String> attributes;

	/**
	 * Creates a web resource from its type name and content.
	 *
	 * @param typeName   A name for the web resource type, for internal use. It must
	 *                   contain only characters matched by the {@code [0-9a-zA-Z_]}
	 *                   regular expression.
	 * @param mimeType   The MIME type for the web resource.
	 * @param attributes A map with the attributes of this web resource, which at
	 *                   least are {@link WebResource#UUID_ATTRIBUTE} and
	 *                   {@link WebResource#CONTENT_ATTRIBUTE}.
	 * @throws IllegalArgumentException If any parameter is {@code null} or invalid.
	 */
	protected WebResource(final String typeName, final String mimeType, final Map<String, String> attributes) {
		if (typeName == null || mimeType == null || attributes == null) {
			throw new IllegalArgumentException(
				"A web resource can't have a null type name, mime type or attributes map"
			);
		}

		if (attributes.get(UUID_ATTRIBUTE) == null || attributes.get(CONTENT_ATTRIBUTE) == null) {
			throw new IllegalArgumentException(
				"A web resource must have at least the UUID_ATTRIBUTE and CONTENT_ATTRIBUTE attributes"
			);
		}

		try {
			UUID.fromString(attributes.get(UUID_ATTRIBUTE));
		} catch (final IllegalArgumentException exc) {
			throw new IllegalArgumentException(
				"The UUID_ATTRIBUTE attribute of a web resource must follow the format of a UUID"
			);
		}

		this.typeName = typeName;
		this.mimeType = mimeType;
		this.attributes = Collections.unmodifiableMap(attributes);
	}

	/**
	 * Retrieves the value of an attribute of this web resource, identified by its
	 * name.
	 *
	 * @param attributeName The name of the attribute whose value is to be returned.
	 * @return The described value. It must be not {@code null}.
	 * @throws IllegalArgumentException If {@code attributeName} is not valid (i.e.,
	 *                                  the set returned by
	 *                                  {@link getAttributeNames} doesn't contain
	 *                                  {@code attributeName}).
	 */
	public final String getAttribute(final String attributeName) {
		if (!attributes.containsKey(attributeName)) {
			throw new IllegalArgumentException(
				"The specified attribute does not exist for this web resource"
			);
		}

		return attributes.get(attributeName);
	}

	/**
	 * Returns a unmodifiable {@link Set} with the names of the attributes that
	 * define this web resource, whose values can be retrieved by calling
	 * {@link getAttribute}. Every web resource returns a set with at least the
	 * {@link WebResource#UUID_ATTRIBUTE} and {@link WebResource#CONTENT_ATTRIBUTE}
	 * attributes. Particular web resources might return more attributes. Each
	 * attribute name must only contain characters matched by the regular expression
	 * {@code [0-9a-zA-Z_-]}.
	 *
	 * @return The described set. The contents of this set will not vary across
	 *         executions of this method.
	 */
	public final Set<String> getAttributeNames() {
		return attributes.keySet();
	}

	/**
	 * Returns the MIME type for this web resource.
	 *
	 * @return The MIME type.
	 */
	public final String getMimeType() {
		return mimeType;
	}

	/**
	 * Returns the name of the web resource type, for internal use. It only contains
	 * characters matched by the {@code [0-9a-zA-Z_]} regular expression.
	 *
	 * @return The described web resource type name.
	 */
	final String getTypeName() {
		return typeName;
	}

	/**
	 * Helper method that converts a list of attributes common for all web resources
	 * to a map. It is best used in subclass constructors.
	 *
	 * @param uuid               The UUID of the web resource.
	 * @param content            The content of the web resource.
	 * @param numberOfAttributes The number of attributes that the web resource has.
	 *                           This number is only used to size data structures
	 *                           for performance, and thus it doesn't need to be
	 *                           exact for the program to operate correctly.
	 * @return The described map, in a modifiable form.
	 */
	protected static Map<String, String> commonAttributesToMap(
		final UUID uuid, final String content, final int numberOfAttributes
	) {
		final Map<String, String> map = new HashMap<>((int) Math.ceil(numberOfAttributes / 0.75));

		map.put(UUID_ATTRIBUTE, uuid == null ? null : uuid.toString());
		map.put(CONTENT_ATTRIBUTE, content);

		return map;
	}

	/**
	 * Creates a web resource of this type from a map that contains its attributes
	 * and values, expressed as strings.
	 *
	 * @param attributes The map that contains the attributes and their values.
	 * @return The new web resource, initialized with the provided attribute values.
	 * @throws IllegalArgumentException If some attribute value is invalid.
	 */
	protected abstract T createFromAttributes(final Map<String, String> attributes);

	@Override
	public final String toString() {
		final String content = getAttribute(CONTENT_ATTRIBUTE);
		return content == null ? getClass().getSimpleName() : content;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Two web resources are equal if their contents are equal and the other web
	 * resource is an instance of the same class that this web resource.
	 */
	@Override
	public final boolean equals(final Object other) {
		return other != null
			&& getClass().equals(other.getClass())
			&& attributes.get(CONTENT_ATTRIBUTE).equals(((WebResource<?>) other).attributes.get(CONTENT_ATTRIBUTE));
	}

	@Override
	public final int hashCode() {
		return attributes.get(CONTENT_ATTRIBUTE).hashCode() ^ getClass().hashCode();
	}
}
