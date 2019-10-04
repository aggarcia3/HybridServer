package es.uvigo.esei.dai.hybridserver;

import java.io.IOException;

public class Launcher {
	// TODO
	public static void main(String[] args) {
		final HybridServer server = new HybridServer();

		System.out.println("-- " + HybridServer.NAME.toUpperCase() + " --");
		server.start();

		System.out.println("Press Enter to shutdown the server.");
		try {
			System.in.read();
		} catch (final IOException exc) {}

		server.stop();
	}
}
