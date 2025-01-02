package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;

import static org.mtcg.ClientHandling.sendResponse;
import static org.mtcg.User.users;

public class SessionHandling {
    public static void handleSessionRequest (String body, OutputStream out) throws IOException{
        String[] userData = body.replaceAll("[{}\"]", "").split(",");
        String username = userData[0].split(":")[1].trim();
        String password = userData[1].split(":")[1].trim();

        boolean usernameExists = false;

        System.out.println("Empfangener Body: " + body);

        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                String userToken = user.getUsername() + "-mtcgToken";
                String jsonResponse = "{\"token\":\"" + userToken + "\"}";
                sendResponse(out, 200, jsonResponse);

                // Konsole: Ausgabe der Informationen
                System.out.println("Username: " + username);
                System.out.println("Password: " + password);
                System.out.println("HTTP 200 - Token returned: " + userToken);
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
    }
//    private static void sendResponse(OutputStream out, int statusCode, String body) throws IOException {
//        String statusLine = "HTTP/1.1 " + statusCode + "\r\n";
//        String httpResponse = statusLine +
//                "Content-Type: application/json\r\n" +
//                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
//                "\r\n" +
//                body;
//        out.write(httpResponse.getBytes("UTF-8"));
//    }
}
