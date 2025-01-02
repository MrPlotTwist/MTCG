package org.mtcg;

import java.io.*;
import java.net.Socket;

import static org.mtcg.User.users;

public class ClientHandling {

    public static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Erste Zeile der Anfrage (Request Line) auslesen
            String requestLine = in.readLine();
            System.out.println("Anfrage: " + requestLine);

            // Sicherstellen, dass die Anfrage eine POST-Anfrage ist
            if (requestLine != null && requestLine.startsWith("POST")) {
                StringBuilder body = new StringBuilder();
                String line;
                String authorizationHeader = null;

                // Kopfzeilen auslesen
                while (!(line = in.readLine()).isEmpty()) {
                    // Header ausgeben, um zu prüfen, was empfangen wurde
                    //System.out.println("Header: " + line);

                    // Authorization-Header suchen
                    if (line.startsWith("Authorization:")) {
                        authorizationHeader = line.substring("Authorization:".length()).trim();
                    }
                }

                // Body lesen, falls vorhanden
                while (in.ready()) {
                    body.append((char) in.read());
                }

                // Debug-Ausgaben
                //System.out.println("Empfangener Authorization-Header: " + authorizationHeader);
                //System.out.println("Body: " + body);

                String[] requestLineParts = requestLine.split(" ");
                String url = requestLineParts[1];

                // Body parsen
//                String[] userData = body.toString().replaceAll("[{}\"]", "").split(",");
//                String username = userData[0].split(":")[1].trim();
//                String password = userData[1].split(":")[1].trim();

                boolean usernameExists = false;

                switch (url) {
                    case "/users":
                        UserHandling.handleUserRequest(body.toString(), out);
                        break;

                    case "/sessions":
                        SessionHandling.handleSessionRequest(body.toString(), out);
                        break;

                    case "/packages":
                        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
                            return;
                        }

                        // Token extrahieren und ausgeben
                        String token = authorizationHeader.substring("Bearer ".length()).trim();
                        System.out.println("Extrahierter Token: " + token);

                        // Anfrage verarbeiten
                        PackageHandling.handlePackageRequest(body.toString(), out);
                        break;

                    case "/transactions/packages":
                        System.out.println("your are here");
                        break;

                    default:
                        sendResponse(out, 400, "{\"message\":\"Bad Request\"}");
                        break;
                }

            } else {
                sendResponse(out, 400, "{\"message\":\"Bad Request\"}");
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

    static void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + "\r\n";
        String httpResponse = statusLine +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        out.write(httpResponse.getBytes("UTF-8"));
    }
}
