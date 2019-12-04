package es.uvigo.esei.dai.hybridserver.xml;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Contains logic for validating XML documents against a XML schema, as defined
 * by the W3C XML Schema 1.0 recommendation.
 *
 * @author Alejandro González García
 */
public final class XMLValidator {
	// Initialization-on-demand holder idiom
	private static final class XMLValidatorInstanceHolder {
		static final XMLValidator INSTANCE = new XMLValidator();
	}

	private XMLValidator() {}

	/**
	 * Gets the only instance in the JVM of this validator.
	 *
	 * @return The instance.
	 */
	public static XMLValidator get() {
		return XMLValidatorInstanceHolder.INSTANCE;
	}

	/**
	 * Checks whether the specified XML document string can be validated
	 * successfully against another XML document that defines a W3C XML Schema 1.0.
	 *
	 * @param xml               The XML to be validated.
	 * @param xsd               The XSD to use to validate the XML against.
	 * @param validationHandler The handler to use for validating the XML. This is
	 *                          useful for retrieving information of the read XML
	 *                          document, for instance.
	 * @return The result of validating the specified XML against the provided XSD.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	public XMLProcessingResult validate(final String xml, final String xsd, final SimpleValidationHandler validationHandler) {
		boolean isValid = true;
		String invalidReason = null;

		if (xml == null || xsd == null || validationHandler == null) {
			throw new IllegalArgumentException(
				"Can't validate a null XML document, or a XML document with a null schema. Also, the validation handler must not be null"
			);
		}

		// Instantiate schema source
        final Source schemaSource = new StreamSource(new StringReader(xsd));

		// Instantiate the schema to use for validation
		try {
			final Schema schema = SchemaFactory.newInstance(
			    XMLConstants.W3C_XML_SCHEMA_NS_URI // W3C XML Schema 1.0 is always supported
			).newSchema(schemaSource);

	        // Now instantiate the SAX parser for that schema
	        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			// "Note that XSLT requires namespace support. Attempting to transform an input
			// source that is not generated with a namespace-aware parser may result in
			// errors. Parsers can be made namespace aware by calling the
			// SAXParserFactory.setNamespaceAware(boolean awareness) method."
	        saxParserFactory.setNamespaceAware(true);
	        // No need to set validating to true. That's only for DTDs:
	        // "This processing will take effect even if the isValidating() method returns false."
	        saxParserFactory.setSchema(schema);

	        // Parser for the XML with the specified XSD
	        final SAXParser saxParser = saxParserFactory.newSAXParser();

	        // Get the XML source document input and parse it
			final InputSource xmlInput = new InputSource(new StringReader(xml));

			saxParser.parse(xmlInput, validationHandler);
		} catch (final IOException | ParserConfigurationException | SAXException exc) {
			// Invalid schema, couldn't create the parser or the XML doesn't validate.
			// Correctness of the XML can't be assured
			isValid = false;
			invalidReason = exc.getMessage();
		}

		return new XMLProcessingResult(isValid, invalidReason);
	}

	/**
	 * Checks whether the specified XML document string can be validated
	 * successfully against another XML document that defines a W3C XML Schema 1.0.
	 *
	 * @param xml               The XML to be validated.
	 * @param xsd               The XSD to use to validate the XML against.
	 * @return The result of validating the specified XML against the provided XSD.
	 * @throws IllegalArgumentException If any parameter is {@code null}.
	 */
	public final XMLProcessingResult validate(final String xml, final String xsd) {
		return validate(xml, xsd, new SimpleValidationHandler());
	}
}
