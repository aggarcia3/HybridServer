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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import static es.uvigo.esei.dai.hybridserver.ServerConfiguration.validateHttpUrl;

/**
 * Represents the configuration parameters of a Hybrid Server.
 */
@XmlRootElement(name = "configuration")
public final class Configuration {
	@XmlElement(name = "connections")
	private ConnectionsConfiguration connectionsConfiguration = new ConnectionsConfiguration();
	@XmlElement(name = "database")
	private DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration();
	@XmlElement(name = "servers")
	private ServersConfiguration serversConfiguration = new ServersConfiguration();

	/**
	 * Initializes a Hybrid Server configuration object with the default
	 * configuration parameters.
	 */
	public Configuration() {}

	/**
	 * Initializes a Hybrid Server configuration object with the provided
	 * configuration parameters.
	 *
	 * @param httpPort      The TCP port the Hybrid Server will listen for HTTP
	 *                      requests on.
	 * @param numClients    A number that estimates the maximum number of concurrent
	 *                      clients this server will be able to handle. It is used
	 *                      to initialize a worker thread pool with this many
	 *                      threads.
	 * @param webServiceURL The endpoint where this Hybrid Server will publish a web
	 *                      service for communication with others.
	 * @param dbUser        The user to specify when logging in the database.
	 * @param dbPassword    The password of the user to specify when logging in the
	 *                      database.
	 * @param dbURI         The JDBC URI of the relational database to connect to.
	 * @param servers       A list of remote Hybrid Server contact information.
	 * @throws IllegalArgumentException If any parameter is {@code null}, except
	 *                                  {@code webServiceURL}, or semantically
	 *                                  invalid.
	 */
	public Configuration(
		final int httpPort,
		final int numClients,
		final String webServiceURL,
		final String dbUser,
		final String dbPassword,
		final String dbURI,
		final List<ServerConfiguration> servers
	) {
		connectionsConfiguration.setHttpPort(httpPort);
		connectionsConfiguration.setNumClients(numClients);
		connectionsConfiguration.setWebServiceURL(webServiceURL);
		databaseConfiguration.setDbUser(dbUser);
		databaseConfiguration.setDbPassword(dbPassword);
		databaseConfiguration.setDbURI(dbURI);
		serversConfiguration.setServers(servers);
	}

	/**
	 * Retrieves the HTTP TCP port the Hybrid Server will listen on for incoming web
	 * resource requests.
	 *
	 * @return The described TCP port.
	 */
	public final int getHttpPort() {
		return connectionsConfiguration.getHttpPort();
	}

	/**
	 * Returns the maximum number of concurrent clients this server will be able to
	 * handle.
	 *
	 * @return The aforementioned number.
	 */
	public final int getNumClients() {
		return connectionsConfiguration.getNumClients();
	}

	/**
	 * Returns the endpoint where this Hybrid Server will publish a web service for
	 * communication with others.
	 *
	 * @return The described endpoint.
	 */
	public final String getWebServiceURL() {
		return connectionsConfiguration.getWebServiceURL();
	}

	/**
	 * Returns the user to specify when logging in the database.
	 *
	 * @return The aforementioned user.
	 */
	public final String getDbUser() {
		return databaseConfiguration.getDbUser();
	}

	/**
	 * Returns the password of the user to specify when logging in the database.
	 *
	 * @return The described password.
	 */
	public final String getDbPassword() {
		return databaseConfiguration.getDbPassword();
	}

	/**
	 * Retrieves the JDBC URI of the relational database to connect to.
	 *
	 * @return The described JDBC URI.
	 */
	public final String getDbURL() {
		return databaseConfiguration.getDbURI();
	}

	/**
	 * Returns the list of remote Hybrid Servers this Hybrid Server knows, providing
	 * their contact information.
	 *
	 * @return The aforementioned list.
	 */
	public final List<ServerConfiguration> getServers() {
		return serversConfiguration.getServers();
	}

	/**
	 * Represents a Hybrid Server configuration fragment, which contains information
	 * relative to the network services a server will provide.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "connections")
	private static final class ConnectionsConfiguration {
		@XmlElement(name = "http")
		private int httpPort = 8888;
		@XmlElement
		private int numClients = 50;
		@XmlElement(name = "webservice")
		private String webServiceURL = null;

		/**
		 * Retrieves the HTTP TCP port the Hybrid Server will listen on for incoming web
		 * resource requests.
		 *
		 * @return The described TCP port.
		 */
		public final int getHttpPort() {
			return httpPort;
		}

		/**
		 * Sets the HTTP TCP port the Hybrid Server will listen on for incoming web
		 * resource requests.
		 *
		 * @param httpPort The aforementioned port.
		 * @throws IllegalArgumentException If the port is not in the interval [1,
		 *                                  65535].
		 */
		final void setHttpPort(final int httpPort) {
			if (httpPort < 1 || httpPort > 65535) {
				throw new IllegalArgumentException("The HTTP port must be between 0 and 65535");
			}

			this.httpPort = httpPort;
		}

		/**
		 * Returns the maximum number of concurrent clients this server will be able to
		 * handle.
		 *
		 * @return The aforementioned number.
		 */
		public final int getNumClients() {
			return numClients;
		}

		/**
		 * Sets the maximum number of concurrent clients this server will be able to
		 * handle.
		 *
		 * @param numClients The described number.
		 * @throws IllegalArgumentException If {@code numClients} is less than 1.
		 */
		final void setNumClients(final int numClients) {
			if (numClients < 1) {
				throw new IllegalArgumentException("The number of clients must not be less than 1");
			}

			this.numClients = numClients;
		}

		/**
		 * Returns the endpoint where this Hybrid Server will publish a web service for
		 * communication with others.
		 *
		 * @return The described endpoint.
		 */
		public final String getWebServiceURL() {
			return webServiceURL;
		}

		/**
		 * Establishes the endpoint where this Hybrid Server will publish a web service
		 * for communication with others.
		 *
		 * @param webServiceURL The aforementioned endpoint.
		 * @throws IllegalArgumentException If {@code webServiceURL} is not an URL, or
		 *                                  it is not a HTTP URL. It can be {@code null}.
		 */
		final void setWebServiceURL(final String webServiceURL) {
			validateHttpUrl(webServiceURL, "web service", true);

			this.webServiceURL = webServiceURL;
		}
	}

	/**
	 * Represents a Hybrid Server configuration fragment, which contains information
	 * relative to the database the server will use for instantiating its DAOs.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "database")
	private static final class DatabaseConfiguration {
		@XmlElement(name = "user")
		private String dbUser = "hsdb";
		@XmlElement(name = "password")
		private String dbPassword = "hsdbpass";
		@XmlElement(name = "url")
		private String dbURI = "jdbc:mysql://localhost:3306/hstestdb";

		/**
		 * Returns the user to specify when logging in the database.
		 *
		 * @return The aforementioned user.
		 */
		public final String getDbUser() {
			return dbUser;
		}

		/**
		 * Sets the user to specify when logging in the database.
		 *
		 * @param dbUser The aforementioned user.
		 * @throws IllegalArgumentException If {@code dbUser} is {@code null}.
		 */
		final void setDbUser(final String dbUser) {
			if (dbUser == null) {
				throw new IllegalArgumentException("The database user can't be null");
			}

			this.dbUser = dbUser;
		}

		/**
		 * Returns the password of the user to specify when logging in the database.
		 *
		 * @return The described password.
		 */
		public final String getDbPassword() {
			return dbPassword;
		}

		/**
		 * Sets the password of the user to specify when logging in the database.
		 *
		 * @param dbPassword The described password.
		 * @throws IllegalArgumentException If {@code dbPassword} is {@code null}.
		 */
		final void setDbPassword(final String dbPassword) {
			if (dbPassword == null) {
				throw new IllegalArgumentException("The database password can't be null");
			}

			this.dbPassword = dbPassword;
		}

		/**
		 * Retrieves the JDBC URI of the relational database to connect to.
		 *
		 * @return The described JDBC URI.
		 */
		public final String getDbURI() {
			return dbURI;
		}

		/**
		 * Sets the JDBC URI of the relational database to connect to.
		 *
		 * @param dbURI The described JDBC URI.
		 * @throws IllegalArgumentException If {@code dbURI} is not an URI, or it is not
		 *                                  a JDBC URI.
		 */
		final void setDbURI(final String dbURI) {
			try {
				if (dbURI == null) {
					throw new URISyntaxException("", "");
				}

				final URI parsedUri = new URI(dbURI);

				if (!parsedUri.getScheme().equals("jdbc")) {
					throw new URISyntaxException("", "");
				}
			} catch (final URISyntaxException exc) {
				throw new IllegalArgumentException("The specified database URI is null or not a JDBC URI");
			}

			this.dbURI = dbURI;
		}
	}

	/**
	 * Represents a Hybrid Server configuration fragment, which contains information
	 * relative to the remote Hybrid Servers that this server will try to
	 * communicate with.
	 *
	 * @author Alejandro González García
	 */
	@XmlRootElement(name = "servers")
	private static final class ServersConfiguration {
		@XmlElements({ @XmlElement(name = "server") })
		private List<ServerConfiguration> servers = null;

		/**
		 * Returns the list of remote Hybrid Servers this Hybrid Server knows, providing
		 * their contact information. The returned list is not modifiable.
		 *
		 * @return The described list of server contact information.
		 */
		public final List<ServerConfiguration> getServers() {
			return servers == null ? Collections.emptyList() : Collections.unmodifiableList(servers);
		}

		/**
		 * Sets the list of remote Hybrid Servers this Hybrid Server knows.
		 *
		 * @param servers The described list of server contact information.
		 * @throws IllegalArgumentException If {@code servers} is {@code null}, or
		 *                                  contains a {@code null} element.
		 */
		final void setServers(final List<ServerConfiguration> servers) {
			if (servers == null || servers.contains(null)) {
				throw new IllegalArgumentException("The server list is null, or a server in the list is null");
			}

			this.servers = servers;
		}
	}
}
