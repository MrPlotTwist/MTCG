package org.mtcg;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static int port;
    private static final int THREAD_POOL_SIZE = 10; // Anzahl der Threads im Pool
    private static ExecutorService threadPool;

    public Server(int port) {
        this.port = port;
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE); // Thread-Pool erstellen
    }

    public static void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server läuft auf Port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket)); // Task an den Thread-Pool übergeben
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // Thread-Pool beim Beenden des Servers schließen
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
          //  System.out.println("Verbunden mit: " + clientSocket.getInetAddress());
            ClientHandling.handleClient(clientSocket); // Client-Logik auslagern
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
