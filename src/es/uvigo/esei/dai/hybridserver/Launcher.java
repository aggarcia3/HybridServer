package es.uvigo.esei.dai.hybridserver;

import java.io.File;
import java.io.IOException;

/**
 * Launches Hybrid Server, loading its configuration parameters.
 *
 * @author Alejandro González García
 */
public class Launcher {
	private static final ConfigurationLoader<?> CONFIGURATION_LOADER = new PropertiesConfigurationLoader();

	/**
	 * Entry point of the application.
	 *
	 * @param args The command line parameters received from the execution
	 *             environment.
	 */
	public static void main(final String[] args) {
		HybridServer server;
		Configuration configuration = null;

		if (args.length <= 1) {
			// Read configuration file if its name is present in arguments
			if (args.length > 0) {
				try {
					// Load the configuration parameters from the file
					configuration = CONFIGURATION_LOADER.load(new File(args[0]));
				} catch (final Exception exc) {
					printErrorMessageAndExit(
						"An error has occured while reading the configuration file: " + exc.getMessage()
					);
				}
			}

			// Initialize the Hybrid Server
			if (configuration != null) {
				server = new HybridServer(configuration);
			} else {
				server = new HybridServer();
			}

			// Start the server
			server.start();

			// Wait for Enter to stop the server
			System.out.println("Press Enter to shutdown the server.");
			try {
				System.in.read();
			} catch (final IOException exc) {
				throw new AssertionError("I/O error while reading from standard input. This shouldn't happen");
			}

			server.stop();

			System.out.println("Goodbye!");
		} else {
			printErrorMessageAndExit("Unexpected number of arguments supplied to the launcher");
		}
	}

	/**
	 * Prints an error message to System.err, and then exits with a nonzero status
	 * code (which signals error).
	 *
	 * @param message The message to show in System.err.
	 */
	private static void printErrorMessageAndExit(final String message) {
		System.err.println(message);
		System.err.println("Syntax: " + Launcher.class.getSimpleName() + " [configuration file]");
		System.exit(1);
	}
}
