/**
 *  HybridServer
 *  Copyright (C) 2017 Miguel Reboiro-Jato
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uvigo.esei.dai.hybridserver;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/**
 * Loads a Hybrid Server configuration from a XML document file.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class XMLConfigurationLoader implements ConfigurationLoader<Void> {
	@Override
	public Configuration load(final File file) throws Exception {

		// Create a unmarshaller that validates the configuration schema
		final Unmarshaller configurationUnmarshaller = JAXBContext.newInstance(Configuration.class).createUnmarshaller();

		// Retrieve the schema source
		final Source schemaSource = new StreamSource(new File("configuration.xsd"));

		configurationUnmarshaller.setSchema(
			SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSource)
		);

		// Do magic :)
		return (Configuration) configurationUnmarshaller.unmarshal(file);
	}
}
