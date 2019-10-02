package es.uvigo.esei.dai.hybridserver;

import java.net.Socket;
import java.io.DataOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class ExecutorThread implements Runnable {
	Socket clientSocket;

	public ExecutorThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

	public void run() {
		try (Socket clientSocket = this.clientSocket) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			DataOutputStream sender = new DataOutputStream(clientSocket.getOutputStream());
			String uuid;
			uuid = reader.readLine();
			/*
			 * Suponemos que pseudoBD es el Map<String,String> que emula a la BD y que está
			 * declarado de forma global
			 */
			if (pseudoBD.contains(uuid)) {
				File file = new File(pseudoBD.get(uuid));
				if (file.isFile() && file.canRead()) {
					try (FileInputStream fileReader = new FileInputStream(fileName)) {
						sender.writeLong(file.length());
						int aux;
						while ((aux = fileReader.read()) != -1) {
							sender.write(aux);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}