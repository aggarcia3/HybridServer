package es.uvigo.esei.dai.hybridserver;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

/**
 * Loads a Hybrid Server configuration from a XML document file, using an
 * implementation of the JAXB specification.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class JAXBConfigurationLoader implements ConfigurationLoader<Void> {
	@Override
	public Configuration load(final File file) throws Exception {
		// Create a unmarshaller that validates the configuration schema
		final Unmarshaller configurationUnmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();

		configurationUnmarshaller.setSchema(
			SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(CONFIGURATION_XSD_FILENAME))
		);

		// Do magic :)
		return (Configuration) configurationUnmarshaller.unmarshal(file);
	}
}
