package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;

import static org.mtcg.ClientHandling.sendResponse;
import static org.mtcg.User.users;

public class UserHandling {

    public static void handleUserRequest(String body, OutputStream out) throws IOException {
        String[] userData = body.replaceAll("[{}\"]", "").split(",");
        String username = userData[0].split(":")[1].trim();
        String password = userData[1].split(":")[1].trim();

        boolean usernameExists = false;

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
