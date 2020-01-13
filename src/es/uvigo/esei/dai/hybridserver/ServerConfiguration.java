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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains the configuration of a remote Hybrid Server, used to establish
 * communication with it.
 */
@XmlRootElement(name = "server")
public final class ServerConfiguration {
	private static final String INCOMPLETE_UNMARSHALLING_ERROR = "A server configuration setting was not unmarshalled successfully";

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
	 * Returns the name of this remote Hybrid Server, unique among the known remote
	 * servers.
	 *
	 * @return The described server name.
	 */
	public final String getName() {
		if (name == null) {
			throw new AssertionError(INCOMPLETE_UNMARSHALLING_ERROR);
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
			throw new AssertionError(INCOMPLETE_UNMARSHALLING_ERROR);
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
			throw new AssertionError(INCOMPLETE_UNMARSHALLING_ERROR);
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
			throw new AssertionError(INCOMPLETE_UNMARSHALLING_ERROR);
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
			throw new AssertionError(INCOMPLETE_UNMARSHALLING_ERROR);
		}

		return httpAddress;
	}
}
