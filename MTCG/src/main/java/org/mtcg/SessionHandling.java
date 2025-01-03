package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mtcg.ClientHandling.sendResponse;

public class SessionHandling {

    public static void handleSessionRequest(String body, OutputStream out) throws IOException {
        // Body parsen
        String[] userData = body.replaceAll("[{}\"]", "").split(",");
        String username = userData[0].split(":")[1].trim();
        String password = userData[1].split(":")[1].trim();

        System.out.println("Empfangener Body: " + body);

        boolean usernameExists = false;

        try (Connection conn = Database.getConnection()) {
            // Benutzer in der Datenbank überprüfen
            String query = "SELECT username FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Benutzer gefunden, Token generieren
                    String userToken = username + "-mtcgToken";
                    String jsonResponse = "{\"token\":\"" + userToken + "\"}";
                    sendResponse(out, 200, jsonResponse);

                    // Konsole: Ausgabe der Informationen
                    System.out.println("Username: " + username);
                    System.out.println("Password: " + password);
                    System.out.println("HTTP 200 - Token returned: " + userToken);
                    usernameExists = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
            return;
        }

        if (!usernameExists) {
            sendResponse(out, 401, "{\"message\":\"Login failed\"}");

            // Konsole: Ausgabe der Informationen
            System.out.println("Username: " + username);
            System.out.println("Password: " + password);
            System.out.println("HTTP 401 - Login failed");
        }
    }
}
