package org.mtcg;

import java.io.OutputStream;
import com.google.gson.Gson;

public class POSTRequestHandling {

    public static void handlePostRequest(String url, String body, String authorizationHeader, OutputStream out) {
        try {
            if (url.equals("/users")) {
                handleUserRequest(body, out);
            } else if (url.equals("/sessions")) {
                handleSessionRequest(body, out);
            } else if (url.equals("/packages")) {
                handlePackageRequest(body, authorizationHeader, out);
            } else if (url.equals("/transactions/packages")) {
                handleTransactionRequest(body, authorizationHeader, out);
            } else if (url.equals("/battles")) {
                handleBattleRequest(authorizationHeader, out);
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

    private static void handleUserRequest(String body, OutputStream out) throws Exception {
        UserHandling.handleUserRequest(body, out);
        System.out.println("POST Body: " + body);
    }

    private static void handleSessionRequest(String body, OutputStream out) throws Exception {
        SessionHandling.handleSessionRequest(body, out);
    }

    private static void handlePackageRequest(String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        // Token extrahieren und ausgeben
        String adminToken = authorizationHeader.substring("Bearer ".length()).trim();
        System.out.println("Extrahierter Token: " + adminToken);

        // Anfrage verarbeiten
        PackageHandling.handlePackageRequest(body, out);
    }

    private static void handleTransactionRequest(String body, String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        // Token extrahieren und Benutzername bestimmen
        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        System.out.println("Extrahierter Token: " + userToken);
        String username = userToken.split("-")[0];

        TransactionHandling.handleTransactionRequest(body, out, username);
    }

    public static void handleBattleRequest(String authorizationHeader, OutputStream out) throws Exception {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
            return;
        }

        String userToken = authorizationHeader.substring("Bearer ".length()).trim();
        String username = userToken.split("-")[0];

        // Füge Spieler zur Battle-Queue hinzu
        Database.addToBattleQueue(username);

        String opponent = Database.getFirstOpponent(username);
        if (opponent == null) {
            sendResponse(out, 200, "{\"message\":\"Waiting for an opponent...\"}");
            return;
        }

        // Entferne Spieler aus der Queue, wenn ein Gegner gefunden wurde
        Database.removeFromQueue(username);
        Database.removeFromQueue(opponent);

        // Starte den Kampf
        BattleResult result = BattleHandling.startBattle(username, opponent);
        sendResponse(out, 200, new Gson().toJson(result));
    }


    private static String findOpponent(String currentUser) {
        // Platzhalter: Implementiere Logik, um einen Gegner aus der Battle-Queue zu finden.
        // Dies könnte die erste andere Person in der Queue sein.
        return Database.getFirstOpponent(currentUser);
    }

    private static void sendResponse(OutputStream out, int statusCode, String body) throws Exception {
        String statusLine = "HTTP/1.1 " + statusCode + "\r\n";
        String httpResponse = statusLine +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + body.getBytes("UTF-8").length + "\r\n" +
                "\r\n" +
                body;
        out.write(httpResponse.getBytes("UTF-8"));
    }
}
