package es.uvigo.esei.dai.hybridserver.webservices;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceException;

import es.uvigo.esei.dai.hybridserver.ServerConfiguration;

/**
 * Contains common logic for the creation and execution of operations on remote
 * Hybrid Servers, via web services.
 *
 * @author Alejandro González García
 */
public final class RemoteServerWebServiceCommunicationController {
	// Initialization-on-demand holder idiom
	private static final class RemoteServerCommunicationControllerInstanceHolder {
		static final RemoteServerWebServiceCommunicationController INSTANCE = new RemoteServerWebServiceCommunicationController();
	}

	private RemoteServerWebServiceCommunicationController() {}

	/**
	 * Gets the only instance in the JVM of this class.
	 *
	 * @return The instance.
	 */
	public static RemoteServerWebServiceCommunicationController get() {
		return RemoteServerCommunicationControllerInstanceHolder.INSTANCE;
	}

	/**
	 * Executes a operation that invokes methods on the service implementation bean
	 * of a remote server, handling any JAX-WS exception that could happen, and
	 * returning a result.
	 *
	 * @param <V>                The type of the returned result.
	 * @param operation          The operation to execute. It can be {@code null},
	 *                           in which case no operation will be executed.
	 * @param remoteServerConfig The remote server to run the operation on.
	 * @param logger             The logger to log errors to, if they happen during
	 *                           the execution of the operations. It can be
	 *                           {@code null}, in which case no logging will be
	 *                           performed.
	 * @return The value that the operation returns. It will be {@code null} if the
	 *         operation returned {@code null}, or an exception occurred.
	 * @throws IllegalArgumentException If {@code remoteServerConfig} is
	 *                                  {@code null}.
	 */
	public <V> V executeRemoteOperation(
		final Function<HybridServerWebService, V> operation, final ServerConfiguration remoteServerConfig, final Logger logger
	) {
		V returnValue = null;

		try {
			if (operation != null) {
				returnValue = operation.apply(
					HybridServerWebServiceEndpointFactory.get().getWebServiceEndpoint(remoteServerConfig)
				);
			}
		} catch (final WebServiceException exc) {
			// Some communication error occurred (socket closed...)
			if (logger != null) {
				logger.log(
					Level.WARNING,
					"An exception has occurred while communicating with a remote Hybrid Server. Is the remote server online?",
					exc
				);
			}
		}

		return returnValue;
	}
}
