package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HybridServer {
	static final String NAME = "Hybrid Server";
	static final Logger LOGGER = Logger.getLogger(NAME);

	private static final int SERVICE_PORT = 8888;
	private static final int STOP_WAIT_MS = 10000;

	// Accesses to this variable should be guarded by serverThreadLock
	private Thread serverThread = null;
	// Accesses to this variable should be guarded by serverReadyLock
	private boolean serverReady = false;
	private final Object serverThreadLock = new Object();
	private final Object serverReadyLock = new Object();

	public HybridServer() {
		// Uncomment to get more verbose logging output
		//Logger.getLogger("").setLevel(Level.ALL);
		//Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);

		// TODO Auto-generated constructor stub
	}

	public HybridServer(Map<String, String> pages) {
		// TODO Auto-generated constructor stub
	}

	public HybridServer(Properties properties) {
		// TODO Auto-generated constructor stub
	}

	public int getPort() {
		return SERVICE_PORT;
	}

	/**
	 * Starts the server service thread, which binds sockets and accepts connections
	 * from clients. This method does not return until the server is ready to accept
	 * connections.
	 */
	public void start() {
		synchronized (serverThreadLock) {
			if (serverThread != null) {
				return;
			}

			serverThread = new Thread() {
				@Override
				public void run() {
					LOGGER.log(Level.INFO, "Starting server", NAME);

					try (final ServerSocketChannel serverSocket = ServerSocketChannel.open().bind(new InetSocketAddress(SERVICE_PORT))) {
						LOGGER.log(Level.INFO, "Listening on {0} for incoming connections", serverSocket.getLocalAddress());

						// Tell other threads we're ready to accept connections
						synchronized (serverReadyLock) {
							serverReady = true;
							serverReadyLock.notifyAll();
						}

						// Keep accepting incoming connections until other thread signals us to stop
						while (!interrupted()) {
							try (final SocketChannel clientSocket = serverSocket.accept()) {
								if (!interrupted()) {
									LOGGER.log(Level.FINE, "Received connection from {0}", clientSocket.getRemoteAddress());

									// Handle the request
									final Socket oldIoClientSocket = clientSocket.socket();
									new HTTPRequestHandlerController(
											oldIoClientSocket.getInputStream(),
											oldIoClientSocket.getOutputStream()
									).handleIncoming();
								}
							} catch (final IOException exc) {
								// Report I/O exceptions, but do not report signals received by other
								// threads to stop
								if (!(exc instanceof ClosedByInterruptException)) {
									LOGGER.log(Level.WARNING, "An I/O error occured while processing a response to a client", exc);
								}
							}
						}
					} catch (final IOException exc) {
						LOGGER.log(Level.SEVERE, "Couldn't bind to port {0}", SERVICE_PORT);
					} finally {
						// Signal ourselves being stopped by clearing the attribute which holds a reference
						// to this thread
						synchronized (serverThreadLock) {
							serverThread = null;
							serverThreadLock.notifyAll();
						}

						// We are not actually be ready to accept connections, but consider
						// ourselves ready as we don't want to make clients wait anymore
						synchronized (serverReadyLock) {
							if (!serverReady) {
								serverReady = true;
								serverReadyLock.notifyAll();
							}
						}
					}
				}
			};

			// Tell the OS scheduler to start the thread.
			// The OS will hopefully start the thread some day...
			serverThread.start();
		}

		// Wait until the server thread is ready to accept connections,
		// so users (and tests) of this method know for sure that we
		// are at least ready to attend connections
		synchronized (serverReadyLock) {
			while (!serverReady) {
				try {
					serverReadyLock.wait();
				} catch (final InterruptedException exc) {
					// Give up
					break;
				}
			}
		}
	}

	public void stop() {
		synchronized (serverThreadLock) {
			// Signal the server thread to stop
			serverThread.interrupt();

			// Wait for the server to stop
			final long waitStart = System.currentTimeMillis();
			final long waitEnd = waitStart + STOP_WAIT_MS;
			while (serverThread != null && System.currentTimeMillis() - waitStart < STOP_WAIT_MS) {
				try {
					serverThreadLock.wait(waitEnd - System.currentTimeMillis());
				} catch (final InterruptedException | IllegalArgumentException exc) {
					// Ignore, we did our best to stop the server
				}
			}

			// Warn the user if the server is still alive
			if (serverThread != null) {
				LOGGER.log(Level.SEVERE, "Couldn't stop the server in a timely manner");
			}
		}
	}
}
