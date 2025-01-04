package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;

public class UserHandling {

    public static void handleUserRequest(String body, OutputStream out) throws IOException {
        try {
            // Body parsen
            String[] userData = body.replaceAll("[{}\"]", "").split(",");
            String username = userData[0].split(":")[1].trim();
            String password = userData[1].split(":")[1].trim();

            System.out.println("Empfangener Body: " + body);

            // Benutzer registrieren
            boolean isRegistered = Database.registerUser(username, password);
            if (isRegistered) {
                sendResponse(out, 201, "{\"message\":\"User created\"}");
                System.out.println("HTTP 201 - User created");
            } else {
                sendResponse(out, 409, "{\"message\":\"User already exists\"}");
                System.out.println("HTTP 409 - User already exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
        }
    }

    private static void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " \r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }
}
