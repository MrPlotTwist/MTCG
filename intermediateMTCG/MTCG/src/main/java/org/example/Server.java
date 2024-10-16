package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static org.example.User.users;

import java.util.ArrayList;
import java.util.List;

public class Server {
    // Liste zur Speicherung von registrierten Benutzern
    private static int port;

    public Server(int port) {
        this.port = port;
    }

    public static void start() {

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

            //POST ANFRAGEN
            if (requestLine != null && requestLine.startsWith("POST")) {
                StringBuilder body = new StringBuilder();
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    // Kopfzeilen überspringen
                }
                //body wird "erstellt"
                while (in.ready()) {
                    body.append((char) in.read());
                }

                String[] requestLineParts = requestLine.split(" ");
                String url = requestLineParts[1];

                String[] userData = body.toString().replaceAll("[{}\"]", "").split(",");
                String username = userData[0].split(":")[1].trim();
                String password = userData[1].split(":")[1].trim();
                boolean usernameExists = false;

                switch (url) {
                    case "/users":
                        System.out.println("Empfangener Body: " + body);

                        User newUser = new User(username, password);

// Prüft, ob Username schon vorhanden ist
                        for (User user : users) {
                            if (user.getUsername().equals(username)) {
                                usernameExists = true;
                                break;
                            }
                        }

// Fügt neuen User hinzu
                        if (!usernameExists) {

                            users.add(newUser);
                            String responseBody = "HTTP 201 - User created\n";
                            int contentLength = responseBody.getBytes("UTF-8").length;

                            String httpResponse = "HTTP/1.1 201\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: " + contentLength + "\r\n" +
                                    "\r\n" +
                                    responseBody;
                            out.write(httpResponse.getBytes("UTF-8"));
                        } else {
                            String responseBody = "HTTP 409 - User already exists\n";
                            int contentLength = responseBody.getBytes("UTF-8").length;

                            String httpResponse = "HTTP/1.1 409\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: " + contentLength + "\r\n" +
                                    "\r\n" +
                                    responseBody;
                            out.write(httpResponse.getBytes("UTF-8"));
                        }
                        break;
                    case "/sessions":
                        System.out.println("Empfangener Body: " + body);

                        for (User user : users) {
                            String userToken = user.getUsername() + "-mtcgToken";
                            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                                String responseBody = "HTTP 200 with generated token for the user, here: " + userToken + "\r\n";
                                int contentLength = responseBody.getBytes("UTF-8").length;
                                String httpResponse = "HTTP/1.1 200\r\n" +
                                        "Content-Type: text/plain\r\n" +
                                        "Content-Length: " + contentLength + "\r\n" +
                                        "\r\n" +
                                        responseBody;
                                out.write(httpResponse.getBytes("UTF-8"));
                                usernameExists = true;
                                break;
                            }
                        }
                        if (!usernameExists) {
                            String responseBody = "HTTP 401 - Login failed\r\n";
                            int contentLength = responseBody.getBytes("UTF-8").length;

                            String httpResponse = "HTTP/1.1 401\r\n" +
                                    "Content-Type: text/plain\r\n" +
                                    "Content-Length: " + contentLength + "\r\n" +
                                    "\r\n" +
                                    responseBody;
                            out.write(httpResponse.getBytes("UTF-8"));
                            break;
                        }
                    default:
                        String httpResponse = "HTTP/1.1 400 Bad Request\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 12\r\n" +
                                "\r\n" +
                                "Bad Request!";
                        out.write(httpResponse.getBytes("UTF-8"));
                        break;
                }

            } else {
                //wenn keine POST request
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
