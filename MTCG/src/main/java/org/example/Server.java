import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        // Port für den Server festlegen
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server läuft auf Port " + port);

            while (true) {
                // Akzeptiere eingehende Verbindungen
                Socket clientSocket = serverSocket.accept();

                // In einem neuen Thread bearbeiten
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                // InputStream und OutputStream des Sockets abrufen
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
        ) {
            // Lese die erste Zeile der HTTP-Anfrage (Request Line)
            String requestLine = in.readLine();
            System.out.println("Anfrage: " + requestLine);

            // Simple Verarbeitung der Anfrage
            if (requestLine != null && requestLine.startsWith("GET")) {
                // Schicke eine einfache HTTP-Response zurück
                String httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 13\r\n" +
                        "\r\n" +
                        "Hello, World!";
                out.write(httpResponse.getBytes("UTF-8"));
            }

        } catch (IOException e) {
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
