package org.mtcg;

import java.io.OutputStream;
import java.util.Map;

import com.google.gson.Gson;

import static org.mtcg.ClientHandling.sendResponse;

public class POSTRequestHandling {

    public static void handlePostRequest(String url, String body, String authorizationHeader, OutputStream out) {
        try {
            if (url.equals("/users")) {
                UserHandling.handleUserRequest(body, out);
            } else if (url.equals("/sessions")) {
                SessionHandling.handleSessionRequest(body, out);
            } else if (url.equals("/packages")) {
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                    sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
                    return;
                }

                // Token extrahieren und ausgeben
                String adminToken = authorizationHeader.substring("Bearer ".length()).trim();
                System.out.println("Extrahierter Token: " + adminToken);

                // Anfrage verarbeiten
                PackageHandling.handlePackageRequest(body, out);
            } else if (url.equals("/transactions/packages")) {
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                    sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
                    return;
                }

                // Token extrahieren und Benutzername bestimmen
                String userToken = authorizationHeader.substring("Bearer ".length()).trim();
                System.out.println("Extrahierter Token: " + userToken);
                String username = userToken.split("-")[0];

                TransactionHandling.handleTransactionRequest(body, out, username);
            } else if (url.equals("/battles")) {
                handleBattleRequest(authorizationHeader, out);
            } else if (url.equals("/tradings")) {
                handleCreateTradingDeal(body, authorizationHeader, out);
            } else if (url.startsWith("/tradings/")) {
                handleTradeRequest(url, body, authorizationHeader, out);
            } else {
                sendResponse(out, 400, "{\"message\":\"Bad Request\"}");
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

    private static final Object battleLock = new Object();

    public static void handleBattleRequest(String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String username = userToken.split("-")[0];
        //System.out.println("[DEBUG] Battle request received from user: " + username);

        synchronized (battleLock) {
            // Füge Spieler zur Battle-Queue hinzu
            Database.addToBattleQueue(username);
            //System.out.println("[DEBUG] User " + username + " added to battle queue");

            String opponent = Database.getFirstOpponent(username);
            if (opponent == null) {
                //System.out.println("[DEBUG] No opponent found for user: " + username);
                sendResponse(out, 200, "{\"message\":\"Waiting for an opponent...\"}");
                return;
            }

            //System.out.println("[DEBUG] Opponent found: " + opponent + " for user: " + username);

            // Entferne Spieler aus der Queue, wenn ein Gegner gefunden wurde
            Database.removeFromQueue(username);
            Database.removeFromQueue(opponent);

//            // Prüfen, ob das Battle schon gestartet wurde
//            boolean battleAlreadyStarted = Database.isBattleActive(username, opponent);
//            if (battleAlreadyStarted) {
//                System.out.println("[DEBUG] Battle between " + username + " and " + opponent + " already in progress.");
//                sendResponse(out, 200, "{\"message\":\"Battle already in progress\"}");
//                return;
//            }

            // Markiere das Battle als aktiv
            Database.markBattleAsActive(username, opponent);

            // Starte den Kampf
            //System.out.println("[DEBUG] Starting battle between " + username + " and " + opponent);
            BattleResult result = BattleHandling.startBattle(username, opponent);
            //System.out.println("[DEBUG] Battle completed between " + username + " and " + opponent);

            // Entferne Battle-Markierung nach Abschluss
            Database.removeBattleMarker(username, opponent);

            // Antwort senden
            sendResponse(out, 200, new Gson().toJson(result));
        }
    }




    private static void handleCreateTradingDeal(String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }
        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String username = userToken.split("-")[0];

        // Body parsen
        Gson gson = new Gson();
        try {
            Map<String, Object> dealData = gson.fromJson(body, Map.class);
            String id = (String) dealData.get("Id");
            String cardId = (String) dealData.get("CardToTrade");
            String type = (String) dealData.get("Type");
            double minDamage = ((Number) dealData.get("MinimumDamage")).doubleValue();

            if (Database.createTradingDeal(id, cardId, type, minDamage, username)) {
                sendResponse(out, 201, "{\"message\":\"Trading deal created successfully\"}");
            } else {
                sendResponse(out, 500, "{\"message\":\"Failed to create trading deal\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 400, "{\"message\":\"Invalid request body\"}");
        }
    }

    private static void handleTradeRequest(String url, String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String username = userToken.split("-")[0];

        // Extrahiere die Trading-Deal-ID aus der URL
        String[] parts = url.split("/");
        if (parts.length < 3) {
            sendResponse(out, 400, "{\"message\":\"Invalid URL format\"}");
            return;
        }
        String dealId = parts[2];

        // Parsen der Karte, die gehandelt werden soll
        Gson gson = new Gson();
        String offeredCardId;
        try {
            offeredCardId = gson.fromJson(body, String.class);
        } catch (Exception e) {
            sendResponse(out, 400, "{\"message\":\"Invalid request body\"}");
            return;
        }

        // Validieren und Handel durchführen
        try {
            System.out.println("DEALID: " + dealId);
            System.out.println("Username: " + username);
            System.out.println("offeredCardId: " + offeredCardId);
            if (Database.processTrade(dealId, username, offeredCardId)) {
                sendResponse(out, 201, "{\"message\":\"Trade successful\"}");
            } else {
                sendResponse(out, 400, "{\"message\":\"Trade failed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
        }
    }
}
