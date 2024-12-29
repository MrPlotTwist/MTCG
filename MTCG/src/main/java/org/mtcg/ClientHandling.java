package org.mtcg;

import java.io.*;
import java.net.Socket;

import static org.mtcg.User.users;

public class ClientHandling {

    public static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String requestLine = in.readLine();
            System.out.println("Anfrage: " + requestLine);

            if (requestLine != null && requestLine.startsWith("POST")) {
                StringBuilder body = new StringBuilder();
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    // Kopfzeilen überspringen
                }
                while (in.ready()) {
                    body.append((char) in.read());
                }

                String[] requestLineParts = requestLine.split(" ");
                String url = requestLineParts[1];

                // Body parsen
                String[] userData = body.toString().replaceAll("[{}\"]", "").split(",");
                String username = userData[0].split(":")[1].trim();
                String password = userData[1].split(":")[1].trim();

                boolean usernameExists = false;

                switch (url) {
                    case "/users":
                        System.out.println("Empfangener Body: " + body);

                        User newUser = new User(username, password);

                        for (User user : users) {
                            if (user.getUsername().equals(username)) {
                                usernameExists = true;
                                break;
                            }
                        }

                        String responseStatus;
                        if (!usernameExists) {
                            users.add(newUser);
                            responseStatus = "HTTP 201 - User created";
                            sendResponse(out, 201, "{\"message\":\"User created\"}");
                        } else {
                            responseStatus = "HTTP 409 - User already exists";
                            sendResponse(out, 409, "{\"message\":\"User already exists\"}");
                        }

                        // Konsole: Ausgabe der Informationen
                        System.out.println("Username: " + username);
                        System.out.println("Password: " + password);
                        System.out.println(responseStatus);
                        break;

                    case "/sessions":
                        System.out.println("Empfangener Body: " + body);

                        for (User user : users) {
                            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                                String userToken = user.getUsername() + "-mtcgToken";
                                String jsonResponse = "{\"token\":\"" + userToken + "\"}";
                                sendResponse(out, 200, jsonResponse);

                                // Konsole: Ausgabe der Informationen
                                System.out.println("Username: " + username);
                                System.out.println("Password: " + password);
                                System.out.println("HTTP 200 - Token returned");
                                usernameExists = true;
                                break;
                            }
                        }

                        if (!usernameExists) {
                            sendResponse(out, 401, "{\"message\":\"Login failed\"}");

                            // Konsole: Ausgabe der Informationen
                            System.out.println("Username: " + username);
                            System.out.println("Password: " + password);
                            System.out.println("HTTP 401 - Login failed");
                        }
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

    private static void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String statusLine = "HTTP/1.1 " + statusCode + "\r\n";
        String httpResponse = statusLine +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        out.write(httpResponse.getBytes("UTF-8"));
    }
}
