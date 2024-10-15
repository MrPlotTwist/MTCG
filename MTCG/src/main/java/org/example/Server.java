package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    // Liste zur Speicherung von registrierten Benutzern
    private static final List<User> users = new ArrayList<>();

    public static void main(String[] args) {
        int port = 10001;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server läuft auf Port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            System.out.println("Anfrage: " + requestLine);

            // Verarbeitung nur für POST-Anfragen
            if (requestLine != null && requestLine.startsWith("POST")) {
                StringBuilder body = new StringBuilder();
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    // Kopfzeilen überspringen
                }
                while (in.ready()) {
                    body.append((char) in.read());
                }

                System.out.println("Empfangener Body: " + body);

                // Parse JSON-Daten und erstelle User
                String[] userData = body.toString().replaceAll("[{}\"]", "").split(",");
                String username = userData[0].split(":")[1].trim();
                String password = userData[1].split(":")[1].trim();

                User newUser = new User(username, password);
                users.add(newUser);
                for (User user : users) {
                    if (user.getUsername().equals(username)) {
                        System.out.println("Username '" + username + "' already exists.");
                        System.out.println("User" + username + "passwort: " + password + " wird entfernt");
                        users.remove(user);
                    }
                }

                // HTTP 201 Created und den User zurückgeben
                String httpResponse = "HTTP/1.1 201 Created\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + newUser.toString().length() + "\r\n" +
                        "\r\n" +
                        newUser.toString();
                out.write(httpResponse.getBytes("UTF-8"));
            } else {
                // Wenn es keine POST-Anfrage ist, sende eine Bad Request-Antwort
                String httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 12\r\n" +
                        "\r\n" +
                        "Bad Request!";
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
