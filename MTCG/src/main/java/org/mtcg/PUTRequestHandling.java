package org.mtcg;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static org.mtcg.ClientHandling.sendResponse;

public class PUTRequestHandling {

    public static void handlePutRequest(String url, String body, String authorizationHeader, OutputStream out) {
        try {
            if (url.equals("/deck")) {
                handleDeckUpdate(body, authorizationHeader, out);
            } else if (url.startsWith("/users/")) {
                String requestedUsername = url.substring("/users/".length());
                handleUserUpdate(requestedUsername, body, authorizationHeader, out);
            } else {
                sendResponse(out, 404, "{\"message\":\"Not Found\"}");
                System.out.println("Ungültige URL: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private static void handleDeckUpdate(String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String username = userToken.split("-")[0];
        System.out.println("Extrahierter Token: " + userToken);

        // JSON-Body parsen
        List<String> cardIds;
        try {
            cardIds = new Gson().fromJson(body, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            sendResponse(out, 400, "{\"message\":\"Invalid request body\"}");
            System.out.println("Ungültiger JSON-Body: " + body);
            return;
        }

        // Prüfen, ob genau 4 Karten angegeben wurden
        if (cardIds.size() != 4) {
            sendResponse(out, 400, "{\"message\":\"Invalid deck size. Exactly 4 cards are required.\"}");
            System.out.println("Ungültige Deckgröße: " + cardIds.size());
            return;
        }

        // Versuche, das Deck zu konfigurieren
        boolean success = Database.configureDeck(username, cardIds);

        if (success) {
            sendResponse(out, 200, "{\"message\":\"Deck successfully configured\"}");
            System.out.println("Deck erfolgreich konfiguriert für Benutzer: " + username);
        } else {
            sendResponse(out, 400, "{\"message\":\"Deck configuration failed. Check card ownership.\"}");
            System.out.println("Deck-Konfiguration fehlgeschlagen für Benutzer: " + username);
        }
    }

    private static void handleUserUpdate(String requestedUsername, String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String authorizedUsername = userToken.split("-")[0];

        // Prüfen, ob der Benutzer seine eigenen Daten aktualisiert
        if (!requestedUsername.equals(authorizedUsername)) {
            sendResponse(out, 403, "{\"message\":\"Forbidden: Cannot update other users' data\"}");
            return;
        }

        // JSON-Body parsen
        Gson gson = new Gson();
        Map<String, String> updates;
        try {
            updates = gson.fromJson(body, Map.class);
        } catch (Exception e) {
            sendResponse(out, 400, "{\"message\":\"Invalid JSON body\"}");
            return;
        }

        // Updates durchführen
        boolean success = Database.updateUserDetails(requestedUsername, updates);
        if (success) {
            sendResponse(out, 200, "{\"message\":\"User data updated successfully\"}");
        } else {
            sendResponse(out, 404, "{\"message\":\"User not found\"}");
        }
    }
}
