package es.uvigo.esei.dai.hybridserver.webservices;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import es.uvigo.esei.dai.hybridserver.ServerConfiguration;

/**
 * Centralizes the retrieval of service implementation beans for remote web
 * services. Although it currently doesn't do such a thing, this abstraction
 * allows recycling instantiated proxies, to avoid the runtime cost of
 * establishing several TCP connections with the same endpoint.
 *
 * @author Alejandro González García
 */
public final class HybridServerWebServiceEndpointFactory {
	// Initialization-on-demand holder idiom
	private static final class HybridServerWebServiceEndpointFactoryInstanceHolder {
		static final HybridServerWebServiceEndpointFactory INSTANCE = new HybridServerWebServiceEndpointFactory();
	}

	private HybridServerWebServiceEndpointFactory() {}

	/**
	 * Gets the only instance in the JVM of this factory.
	 *
	 * @return The instance.
	 */
	public static HybridServerWebServiceEndpointFactory get() {
		return HybridServerWebServiceEndpointFactoryInstanceHolder.INSTANCE;
	}

	/**
	 * Returns the service implementation bean of a remote server, which can be used
	 * like a local object to invoke data access methods on the remote server.
	 *
	 * @param remoteServerConfig The communication parameters for the remote server,
	 *                           obtained from the configuration file.
	 * @return The described SIB. Operations on this SIB may throw
	 *         {@code WebServiceException} if some error occurs at the JAX-WS layer.
	 * @throws WebServiceException If the connection with the remote server couldn't
	 *                             be established, or another JAX-WS exception
	 *                             occurred.
	 * @throws IllegalArgumentException If {@code remoteServerConfig} is {@code null}.
	 */
	public HybridServerWebService getWebServiceEndpoint(final ServerConfiguration remoteServerConfig) {
		if (remoteServerConfig == null) {
			throw new IllegalArgumentException(
				"Can't get a service implementation bean for a null remote server configuration"
			);
		}

		try {
			return Service.create(
				new URL(remoteServerConfig.getWsdl()),
				new QName(remoteServerConfig.getNamespace(), remoteServerConfig.getService())
			).getPort(HybridServerWebService.class);
		} catch (final MalformedURLException exc) {
			throw new AssertionError(exc);
		}
	}
}
