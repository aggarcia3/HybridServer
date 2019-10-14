package es.uvigo.esei.dai.hybridserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Launches Hybrid Server, loading its configuration parameters.
 *
 * @author Alejandro González García
 */
public class Launcher {
	/**
	 * Entry point of the application.
	 *
	 * @param args The command line parameters received from the execution
	 *             environment.
	 */
	public static void main(final String[] args) {
		final Properties configuration = new Properties();
		HybridServer server;

		if (args.length == 1) {
			try (final Reader configurationReader = new InputStreamReader(new FileInputStream(args[1]), StandardCharsets.UTF_8)) {
				// Load the configuration properties from the reader
				configuration.load(configurationReader);

				// Initialize the Hybrid Server with the configuration
				server = new HybridServer(configuration);

				// Start it
				server.start();

				// Wait for Enter to stop the server
				System.out.println("Press Enter to shutdown the server.");
				try {
					System.in.read();
				} catch (final IOException exc) {
					throw new AssertionError("I/O error while reading from standard input. This shouldn't happen");
				}

				server.stop();
			} catch (final FileNotFoundException exc) {
				printErrorMessageAndExit("Unable to open the file " + args[1] + " for reading. Does it exist, and is it a file?");
			} catch (final IOException exc) {
				printErrorMessageAndExit("An I/O error has occured while reading the configuration file");
			}
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
		System.err.println("Syntax: " + Launcher.class.getSimpleName() + " (configuration file)");
		System.exit(1);
	}
}
