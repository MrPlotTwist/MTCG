package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mtcg.ClientHandling.sendResponse;

public class UserHandling {

    public static void handleUserRequest(String body, OutputStream out) throws IOException {
        // Body parsen
        String[] userData = body.replaceAll("[{}\"]", "").split(",");
        String username = userData[0].split(":")[1].trim();
        String password = userData[1].split(":")[1].trim();

        System.out.println("Empfangener Body: " + body);

        String responseStatus = "";

        try (Connection conn = Database.getConnection()) {
            // Überprüfen, ob der Benutzername bereits existiert
            String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                rs.next(); // ResultSet auf die erste Zeile bewegen

                if (rs.getInt(1) > 0) {
                    responseStatus = "HTTP 409 - User already exists";
                    sendResponse(out, 409, "{\"message\":\"User already exists\"}");
                    System.out.println(responseStatus);
                    return;
                }
            }

            // Neuen Benutzer einfügen
            String insertUserQuery = "INSERT INTO users (username, password, coins) VALUES (?, ?, 20)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
            }

            responseStatus = "HTTP 201 - User created";
            sendResponse(out, 201, "{\"message\":\"User created\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
        }

        // Konsole: Ausgabe der Informationen
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        System.out.println(responseStatus);
    }
}
