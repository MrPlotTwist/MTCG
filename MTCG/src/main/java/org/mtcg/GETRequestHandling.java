package org.mtcg;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.mtcg.ClientHandling.sendResponse;

public class GETRequestHandling {

    public static void handleGetRequest(String url, String authorizationHeader, OutputStream out) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
                System.out.println("INVALID TOKENNNN!");
                return;
            }

            // Extrahiere Token und Benutzername
            String userToken = authorizationHeader.substring("Bearer ".length()).trim();
            String username = userToken.split("-")[0];
            System.out.println("Extrahierter Token: " + userToken);
            System.out.println("Benutzername: " + username);

            // URL in Pfad und Query-Parameter trennen
            String path = url.split("\\?")[0]; // Extrahiere den Pfad (z.B. "/deck")
            String query = url.contains("?") ? url.split("\\?")[1] : ""; // Extrahiere Query-Parameter (z.B. "format=plain")

            // Verarbeite den Pfad
            if (path.equals("/cards")) {
                handleCardsRequest(username, out);
            } else if (path.equals("/deck")) {
                handleDeckRequest(username, query, out);
            } else if (path.startsWith("/users/")) {
                String requestedUsername = path.substring("/users/".length());
                handleUserRequest(requestedUsername, username, out);
            } else if (url.equals("/stats")) {
                handleStatsRequest(username, out);
            } else if (url.equals("/scoreboard")) {
                handleScoreboardRequest(out);
            } else if (url.equals("/tradings")) {
                handleGetTradingDeals(out);
            } else {
                sendResponse(out, 404, "{\"message\":\"Not Found\"}");
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

    private static void handleCardsRequest(String username, OutputStream out) {
        try {
            // Karten des Benutzers abrufen
            List<String> cards = Database.getCardsForUser(username);

            // Prüfen, ob Karten gefunden wurden
            if (cards == null || cards.isEmpty()) {
                sendResponse(out, 404, "{\"message\":\"No cards found for user: " + username + "\"}");
            } else {
                // Karten als JSON-Antwort senden
                StringBuilder responseBody = new StringBuilder("{\"cards\": [");
                for (int i = 0; i < cards.size(); i++) {
                    responseBody.append("\"").append(cards.get(i)).append("\"");
                    if (i < cards.size() - 1) {
                        responseBody.append(", ");
                    }
                }
                responseBody.append("]}");
                sendResponse(out, 200, responseBody.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleDeckRequest(String username, String query, OutputStream out) {
        try {
            // Deck des Benutzers abrufen
            List<String> deck = Database.getDeckForUser(username);

            // Prüfen, ob das Deck leer ist
            if (deck == null || deck.isEmpty()) {
                sendResponse(out, 200, "[]");
            } else if ("format=plain".equals(query)) {
                // Deck im Klartextformat senden
                StringBuilder plainResponse = new StringBuilder();
                for (String card : deck) {
                    plainResponse.append(card).append("\n");
                }
                sendResponse(out, 200, plainResponse.toString().trim());
            } else {
                // Deck als JSON-Antwort senden
                StringBuilder responseBody = new StringBuilder("{\"deck\": [");
                for (int i = 0; i < deck.size(); i++) {
                    responseBody.append("\"").append(deck.get(i)).append("\"");
                    if (i < deck.size() - 1) {
                        responseBody.append(", ");
                    }
                }
                responseBody.append("]}");
                sendResponse(out, 200, responseBody.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleUserRequest(String requestedUsername, String authorizedUsername, OutputStream out) {
        try {
            if (!requestedUsername.equals(authorizedUsername)) {
                sendResponse(out, 403, "{\"message\":\"Forbidden: Cannot access other users' data\"}");
                return;
            }

            // Benutzerdaten abrufen
            User user = Database.getUserData(requestedUsername);
            if (user != null) {
                sendResponse(out, 200, new Gson().toJson(user));
            } else {
                sendResponse(out, 404, "{\"message\":\"User not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void handleStatsRequest(String username, OutputStream out) throws Exception {
        // Hole Benutzerstatistiken aus der Datenbank
        UserStats stats = Database.getUserStats(username);

        if (stats != null) {
            sendResponse(out, 200, new Gson().toJson(stats));
        } else {
            sendResponse(out, 404, "{\"message\":\"Stats not found for user: " + username + "\"}");
        }
    }
    private static void handleScoreboardRequest(OutputStream out) throws Exception {
        // Hole das Scoreboard aus der Datenbank
        List<UserStats> scoreboard = Database.getScoreboard();

        if (scoreboard != null && !scoreboard.isEmpty()) {
            // Füge die Platzierung hinzu
            for (int i = 0; i < scoreboard.size(); i++) {
                scoreboard.get(i).setRank(i + 1);
            }

            sendResponse(out, 200, new Gson().toJson(scoreboard));
        } else {
            sendResponse(out, 404, "{\"message\":\"Scoreboard is empty\"}");
        }
    }
    private static void handleGetTradingDeals(OutputStream out) throws IOException {
        List<Trading> deals = Database.getTradingDeals();
        Gson gson = new Gson();
        String response = gson.toJson(deals);
        sendResponse(out, 200, response);
    }


}
