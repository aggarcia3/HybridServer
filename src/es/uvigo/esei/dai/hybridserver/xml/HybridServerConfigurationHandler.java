package es.uvigo.esei.dai.hybridserver.xml;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import es.uvigo.esei.dai.hybridserver.Configuration;
import es.uvigo.esei.dai.hybridserver.ServerConfiguration;

/**
 * Handles the parsing of a Hybrid Server XML configuration file, instantiating
 * the resulting {@link Configuration} object.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is not thread-safe.
 */
public final class HybridServerConfigurationHandler extends DefaultHandler {
	private Configuration configuration = null;

	private ElementContext elementContext = null;
	private final StringBuilder elementCharactersBuilder = new StringBuilder(64); // JDBC URIs are a bit long

	private int httpPort = 0;
	private int numClients = 0;
	private String webServiceURL = null;
	private String dbUser = null;
	private String dbPassword = null;
	private String dbUrl = null;
	private List<ServerConfiguration> servers = new ArrayList<>();

	private boolean parseError = false;

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		try {
			// Change the element context, which stores the element being parsed and how should it be parsed
			elementContext = ElementContext.valueOf(localName.toUpperCase());
			elementContext.setConfigurationHandler(this);

			// Interpret element attributes if needed
			if (elementContext.interpretsAttributes()) {
				elementContext.interpretAttributes(attributes);
			}

			// Discard data in string builder, but preserve its capacity
			if (elementContext.interpretsCharacterData()) {
				elementCharactersBuilder.setLength(0);
			}
		} catch (final IllegalArgumentException exc) {
			// We entered an irrelevant context
			elementContext = null;
		} catch (final NullPointerException exc) {
			// This shouldn't happen
			parseError = true;
			throw new SAXException(exc);
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if (elementContext != null) {
			final ElementContext previousContext = elementContext;

			// Clear the element context
			elementContext = null;

			// Interpret characters as needed
			if (previousContext.interpretsCharacterData()) {
				try {
					previousContext.interpretCharacterData(elementCharactersBuilder.toString());
				} catch (final SAXException exc) {
					parseError = true;
					throw exc;
				}
			}
		}
	}

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		if (elementContext != null && elementContext.interpretsCharacterData()) {
			// "The Parser will call this method to report each chunk of character data. SAX parsers may return all
			// contiguous character data in a single chunk, or they may split it into several chunks"
			// Therefore, to play it safe, we need to be able to concatenate arbitrary chunks of character data
			elementCharactersBuilder.append(ch, start, length);
		}
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			this.configuration = new Configuration(httpPort, numClients, webServiceURL, dbUser, dbPassword, dbUrl, servers); 
		} catch (final IllegalArgumentException exc) {
			parseError = true;
			throw new SAXException(exc);
		}
	}

	@Override
	public void warning(final SAXParseException exc) throws SAXException {
		// Treat warnings as fatal errors and stop parsing
		parseError = true;
		throw exc;
	}

	@Override
	public void error(final SAXParseException exc) throws SAXException {
		// Treat errors (for instance, schema non-compliance) as fatal errors and stop
		parseError = true;
		throw exc;
	}

	/**
	 * Gets the configuration object that represents what has been read from the XML
	 * configuration file. This method should be called only after this handler has
	 * parsed a XML file successfully.
	 *
	 * @return The described {@link Configuration}. It is not {@code null}.
	 * @throws IllegalStateException If this handler didn't parse a XML file yet, or
	 *                               it threw an exception during parsing.
	 */
	public Configuration configurationRead() {
		// "There is an apparent contradiction between the documentation for this method and the
		// documentation for ContentHandler.endDocument(). Until this ambiguity is resolved in a
		// future major release, clients should make no assumptions about whether endDocument() will
		// or will not be invoked when the parser has reported a fatalError() or thrown an exception."
		// This means that endDocument() might be invoked, and a Configuration object created, even though
		// we failed on the last steps of parsing (just before <servers>, for instance). The boolean flag
		// guards against that and doesn't let that invalid configuration pass through
		if (configuration == null || parseError) {
			throw new IllegalStateException(
				"Can't get the configuration from a configuration handler when it didn't read the configuration"
			);
		}

		return configuration;
	}

	/**
	 * Represents a element context for the XML parser, which is active while the
	 * parser is parsing character data inside an element. This context decides how
	 * to deal with that character data and the attributes of the element.
	 *
	 * @author Alejandro González García
	 */
	private static enum ElementContext {
		/**
		 * The context for the {@code <http>} element.
		 */
		HTTP {
			@Override
			public void interpretCharacterData(final String string) {
				handler.httpPort = Integer.parseInt(string);
			}
		},
		/**
		 * The context for the {@code <webservice>} element.
		 */
		WEBSERVICE {
			@Override
			public void interpretCharacterData(final String string) {
				handler.webServiceURL = string;
			}
		},
		/**
		 * The context for the {@code <numClients>} element.
		 */
		NUMCLIENTS {
			@Override
			public void interpretCharacterData(final String string) {
				handler.numClients = Integer.parseInt(string);
			}
		},
		/**
		 * The context for the {@code <user>} element.
		 */
		USER {
			@Override
			public void interpretCharacterData(final String string) {
				handler.dbUser = string;
			}
		},
		/**
		 * The context for the {@code <password>} element.
		 */
		PASSWORD {
			@Override
			public void interpretCharacterData(final String string) {
				handler.dbPassword = string;
			}
		},
		/**
		 * The context for the {@code <url>} element.
		 */
		URL {
			@Override
			public void interpretCharacterData(final String string) {
				handler.dbUrl = string;
			}
		},
		/**
		 * The context for the {@code <server>} element.
		 */
		SERVER {
			@Override
			public void interpretCharacterData(final String string) {
				// Nothing to do here
			}

			@Override
			public boolean interpretsCharacterData() {
				return false;
			}

			@Override
			public void interpretAttributes(final Attributes attributes) throws SAXException {
				try {
					handler.servers.add(
						new ServerConfiguration(
							attributes.getValue("name"), attributes.getValue("wsdl"),
							attributes.getValue("namespace"), attributes.getValue("service"),
							attributes.getValue("httpAddress")
						)
					);
				} catch (final IllegalArgumentException exc) {
					throw new SAXException(exc);
				}
			}

			@Override
			public boolean interpretsAttributes() {
				return true;
			}
		};

		protected HybridServerConfigurationHandler handler = null;

		/**
		 * Sets the configuration handler whose state will be altered by this context.
		 *
		 * @param handler The described handler. It must not be {@code null}.
		 */
		public final void setConfigurationHandler(final HybridServerConfigurationHandler handler) {
			this.handler = handler;
		}

		/**
		 * Interprets the character data inside a element as appropriate, given the
		 * context this method is invoked on.
		 *
		 * @param string The string that contains the described character data. It is
		 *               assumed to be not {@code null}.
		 * @throws SAXException If it is not possible to interpret that character data.
		 */
		public abstract void interpretCharacterData(final String string) throws SAXException;

		/**
		 * Returns whether this context interprets character data; that is, whether it
		 * does something useful with it, or it can be ignored.
		 *
		 * @return The truth value of the described proposition.
		 */
		public boolean interpretsCharacterData() {
			return true;
		}

		/**
		 * Interprets the attributes of an element, given the context this method is
		 * invoked on.
		 *
		 * @param attributes The attributes to interpret. They are assumed to be not
		 *                   {@code null}.
		 * @throws SAXException If it is not possible to interpret the attributes.
		 * @implNote The default implementation of this method does nothing. Subclasses
		 *           willing to interpret attributes must override this method and
		 *           {@link ElementContext#interpretsAttributes}.
		 */
		public void interpretAttributes(final Attributes attributes) throws SAXException {
			// Do nothing
		}

		/**
		 * Returns whether this context interprets attributes of elements; that is,
		 * whether it does something useful with them, or they can be ignored.
		 *
		 * @return The truth value of the described proposition.
		 * @implNote The default implementation of this method returns {@code false}.
		 * @see ElementContext#interpretAttributes
		 */
		public boolean interpretsAttributes() {
			return false;
		}
	}
}
