package org.mtcg;

import java.io.OutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mtcg.ClientHandling.sendResponse;

public class SessionHandling {

    public static void handleSessionRequest(String body, OutputStream out) throws IOException {
        String[] userData = body.replaceAll("[{}\"]", "").split(",");
        String username = userData[0].split(":")[1].trim();
        String password = userData[1].split(":")[1].trim();

        if (Database.login(username, password)) {
            String token = username + "-mtcgToken";
            sendResponse(out, 200, "{\"token\":\"" + token + "\"}");
        } else {
            sendResponse(out, 401, "{\"message\":\"Login failed\"}");
        }
    }
}
