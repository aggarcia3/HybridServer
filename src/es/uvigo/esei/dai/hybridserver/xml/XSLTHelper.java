package es.uvigo.esei.dai.hybridserver.xml;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import es.uvigo.esei.dai.hybridserver.http.MIME;
import es.uvigo.esei.dai.hybridserver.webresource.XMLWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSDWebResource;
import es.uvigo.esei.dai.hybridserver.webresource.XSLTWebResource;

/**
 * Provides methods for easily applying XSLT stylesheets to XML documents,
 * validating them with a schema, using the web resources abstractions.
 *
 * @author Alejandro González García
 */
public final class XSLTHelper {
	// Initialization-on-demand holder idiom
	private static final class XSLTHelperInstanceHolder {
		static final XSLTHelper INSTANCE = new XSLTHelper();
	}

	private XSLTHelper() {}

	/**
	 * Gets the only instance in the JVM of this helper.
	 *
	 * @return The instance.
	 */
	public static XSLTHelper get() {
		return XSLTHelperInstanceHolder.INSTANCE;
	}

	/**
	 * Transforms a source XML document web resource with a XSLT stylesheet,
	 * validating the XML document with a XML schema (XSD web resource) before the
	 * actual transformation takes place. This method supports all the XSLT
	 * transformation methods defined in the
	 * <a href="https://www.w3.org/TR/xslt-10/#output">W3C XSLT 1.0
	 * Recommendation</a>, namely {@code xml}, {@code html} and {@code text}. Any
	 * other transformation method will work, providing the underlying XSLT
	 * transformer implementation supports it, but its resulting MIME type will be
	 * that of a {@code text} transformation.
	 *
	 * @param xml  The XML document web resource that will be transformed.
	 * @param xsd  The XML schema web resource to use to validate the source XML
	 *             document.
	 * @param xslt The XSLT stylesheet to apply to the XML document, if the XML
	 *             document was validated successfully with the provided schema.
	 * @return The result of transforming the source XML document with the specified
	 *         XSLT. Any checked exception occurred during parsing or transforming
	 *         will be reflected in this result object.
	 */
	public XSLTTransformResult transform(final XMLWebResource xml, final XSDWebResource xsd, final XSLTWebResource xslt) {
		XMLProcessingResult processingResult;
		MIME transformResultMime = null;
		String transformResultContent = null;

		if (xml == null || xsd == null || xslt == null) {
			throw new IllegalArgumentException(
				"Can't apply a XSLT stylesheet with a null XML, XSD or XSLT"
			);
		}

		// Validate the XML before transforming it
		processingResult = XMLValidator.get().validate(
			xml.getAttribute(XMLWebResource.CONTENT_ATTRIBUTE),
			xsd.getAttribute(XSDWebResource.CONTENT_ATTRIBUTE)
		);

		if (processingResult.wasSuccessful()) {
			try {
				// It is valid XML, so transform it
				final Transformer xsltTransformer = TransformerFactory.newInstance().newTransformer(
					new StreamSource(
						new StringReader(xslt.getAttribute(XSLTWebResource.CONTENT_ATTRIBUTE))
					)
				);

				// Get the MIME type of the transformation result
				switch (xsltTransformer.getOutputProperty(OutputKeys.METHOD)) {
					case "xml":
						transformResultMime = MIME.APPLICATION_XML;
						break;
					case "html":
						transformResultMime = MIME.TEXT_HTML;
						break;
					case "text":
					default:
						transformResultMime = MIME.TEXT_PLAIN;
						break;
				}

				// Perform the transformation and store its result
				final StringWriter transformationResultWriter = new StringWriter(
					xml.getAttribute(XMLWebResource.CONTENT_ATTRIBUTE).length()
				);

				xsltTransformer.transform(
					new StreamSource(
						new StringReader(xml.getAttribute(XMLWebResource.CONTENT_ATTRIBUTE))
					),
					new StreamResult(transformationResultWriter)
				);

				// Transformation done
				transformResultContent = transformationResultWriter.toString();
			} catch (final TransformerException exc) {
				processingResult = new XMLProcessingResult(false, exc.getMessageAndLocation());
				transformResultMime = null;
			}
		}

		return new XSLTTransformResult(processingResult, transformResultMime, transformResultContent);
	}
}
