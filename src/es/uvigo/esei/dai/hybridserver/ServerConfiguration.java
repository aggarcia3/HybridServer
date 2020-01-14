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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains the configuration of a remote Hybrid Server, used to establish
 * communication with it.
 */
@XmlRootElement(name = "server")
public final class ServerConfiguration {
	private static final String INCOMPLETE_INITIALIZATION_ERROR = "A server configuration setting was not unmarshalled successfully";

	@XmlAttribute
	private String name = null;
	@XmlAttribute
	private String wsdl = null;
	@XmlAttribute
	private String namespace = null;
	@XmlAttribute
	private String service = null;
	@XmlAttribute
	private String httpAddress = null;

	/**
	 * This constructor is only provided to allow the JAXB framework to instantiate
	 * the class, setting up the new instance with invalid state that it is expected
	 * to be fixed by the usage of reflection. Therefore, most users of this class
	 * will find no use for this constructor and should use the other one.
	 */
	public ServerConfiguration() {}

	/**
	 * Creates a new server configuration instance, representing a remote Hybrid
	 * Server susceptible outgoing communications with it.
	 *
	 * @param name        The identifying name of the Hybrid Server. It must be
	 *                    unique, but this constructor does not check that, so it is
	 *                    the responsibility of the users of this constructors to
	 *                    guarantee that.
	 * @param wsdl        The HTTP URL where the WSDL of the Hybrid Server is
	 *                    available for download.
	 * @param namespace   The namespace where the remote Hybrid Server web service
	 *                    is defined.
	 * @param service     The name of the web service offered by the Hybrid Server.
	 * @param httpAddress The base HTTP address of the Hybrid Server, where its
	 *                    welcome page is reachable.
	 * @throws IllegalArgumentException If some argument is {@code null} or
	 *                                  semantically invalid.
	 */
	public ServerConfiguration(final String name, final String wsdl, final String namespace, final String service, final String httpAddress) {
		if (name == null || wsdl == null || namespace == null || service == null || httpAddress == null) {
			throw new IllegalArgumentException("Can't create a server configuration with a null parameter");
		}

		if (name.isEmpty()) {
			throw new IllegalArgumentException("The server name must not be empty");
		}

		validateHttpUrl(wsdl, "WSDL", false);
		validateHttpUrl(namespace, "namespace", false);

		if (service.isEmpty()) {
			throw new IllegalArgumentException("The service name must not be empty");
		}

		validateHttpUrl(httpAddress, "HTTP address", false);

		this.name = name;
		this.wsdl = wsdl;
		this.namespace = namespace;
		this.service = service;
		this.httpAddress = httpAddress;
	}

	/**
	 * Returns the name of this remote Hybrid Server, unique among the known remote
	 * servers.
	 *
	 * @return The described server name.
	 */
	public final String getName() {
		if (name == null) {
			throw new AssertionError(INCOMPLETE_INITIALIZATION_ERROR);
		}

		return name;
	}

	/**
	 * Returns the WSDL endpoint of the remote Hybrid Server.
	 *
	 * @return The described endpoint.
	 */
	public final String getWsdl() {
		if (wsdl == null) {
			throw new AssertionError(INCOMPLETE_INITIALIZATION_ERROR);
		}

		return wsdl;
	}

	/**
	 * Returns the namespace of the remote Hybrid Server web service.
	 * 
	 * @return The described namespace.
	 */
	public final String getNamespace() {
		if (namespace == null) {
			throw new AssertionError(INCOMPLETE_INITIALIZATION_ERROR);
		}

		return namespace;
	}

	/**
	 * Retrieves the service name of the remote Hybrid Server web service.
	 *
	 * @return The aforementioned service name.
	 */
	public final String getService() {
		if (service == null) {
			throw new AssertionError(INCOMPLETE_INITIALIZATION_ERROR);
		}

		return service;
	}

	/**
	 * Returns the base HTTP address where the remote Hybrid Server serves web
	 * resources.
	 *
	 * @return The aforementioned base HTTP address.
	 */
	public final String getHttpAddress() {
		if (httpAddress == null) {
			throw new AssertionError(INCOMPLETE_INITIALIZATION_ERROR);
		}

		return httpAddress;
	}

	/**
	 * Parses the specified string as a HTTP URL, throwing a exception if it is not
	 * a HTTP URL.
	 *
	 * @param url             The URL to parse as a HTTP URL.
	 * @param descriptiveName A name for the URL, that describes the resource it
	 *                        represents.
	 * @param canBeNull       If {@code true}, then {@code null} will be considered
	 *                        as a valid HTTP URL. Otherwise, {@code null} URLs are
	 *                        invalid.
	 * @throws IllegalArgumentException If {@code url} is not a valid HTTP URL.
	 */
	static void validateHttpUrl(final String url, final String descriptiveName, final boolean canBeNull) {
		try {
			if (url != null) {
				final URL parsedUrl = new URL(url);

				if (!parsedUrl.getProtocol().equals("http")) {
					throw new MalformedURLException();
				}
			} else if (!canBeNull) {
				throw new MalformedURLException();
			}
		} catch (final MalformedURLException exc) {
			throw new IllegalArgumentException("The specified " + descriptiveName + " URL is not a HTTP URL");
		}
	}
}
