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
import java.io.FileInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.InputSource;

import es.uvigo.esei.dai.hybridserver.xml.HybridServerConfigurationHandler;

/**
 * Loads a Hybrid Server configuration from a XML document file, using a SAX
 * parser.
 *
 * @author Alejandro González García
 * @implNote The implementation of this class is thread-safe.
 */
public final class XMLConfigurationLoader implements ConfigurationLoader<InputSource> {
	@Override
	public Configuration load(final File file) throws Exception {
		// "However, standard processing of both byte and character streams is to close them on as part of end-of-parse cleanup"
		// No need to close them ourselves
		return load(new InputSource(new FileInputStream(file)));
	}

	@Override
	public Configuration load(final InputSource input) throws Exception {
		final SAXParserFactory parserFactory = SAXParserFactory.newInstance();

		// Get the schema to validate the configuration with, and parse it
		final Schema schema = SchemaFactory.newInstance(
			XMLConstants.W3C_XML_SCHEMA_NS_URI
		).newSchema(new File(CONFIGURATION_XSD_FILENAME));

		parserFactory.setNamespaceAware(true);
		parserFactory.setSchema(schema);

		final SAXParser parser = parserFactory.newSAXParser();
		final HybridServerConfigurationHandler handler = new HybridServerConfigurationHandler();
		parser.parse(input, handler);

		// Ask the handler for the result
		return handler.configurationRead();
	}

	@Override
	public boolean supportsNativeFormatLoading() {
		return true;
	}
}
