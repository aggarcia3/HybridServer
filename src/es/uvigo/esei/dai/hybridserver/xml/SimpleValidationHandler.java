package es.uvigo.esei.dai.hybridserver.xml;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Provides a simple schema validation handler that treats parsing warnings nad
 * errors as fatal errors, so the SAX parser is aborted whenever a schema
 * validation constraint fails. For the rest, it behaves like a
 * {@link DefaultHandler}.
 *
 * @author Alejandro González García
 */
public class SimpleValidationHandler extends DefaultHandler {
	@Override
	public final void warning(final SAXParseException exc) throws SAXException {
		handleAbort(exc);

		// Treat validation warnings as fatal errors, and abort the execution of the parser
		throw exc;
	}

	@Override
	public final void error(final SAXParseException exc) throws SAXException {
		handleAbort(exc);

		// Treat validation errors as fatal errors, and abort the execution of the parser
		throw exc;
	}

	@Override
	public final void fatalError(final SAXParseException exc) throws SAXException {
		handleAbort(exc);

		throw exc;
	}

	/**
	 * Provides an extension point for subclasses, so they can react as they please
	 * when the parser execution is aborted due to an error, warning or fatal error.
	 * This design ensures that the parser execution is always aborted, no matter if
	 * subclasses throw an exception here or not. The default implementation of this
	 * method does nothing.
	 *
	 * @param exc The warning, error or fatal error that is about to abort the
	 *            execution of the parser.
	 */
	protected void handleAbort(final SAXParseException exc) {
		// Do nothing
	}
}
