package org.mtcg;

import java.io.OutputStream;

import static org.mtcg.ClientHandling.sendResponse;

public class DELETERequestHandling {

    public static void handleDeleteRequest(String url, String authorizationHeader, OutputStream out) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                sendResponse(out, 401, "{\"message\":\"Unauthorized: Missing or invalid token\"}");
                return;
            }

            String userToken = authorizationHeader.substring("Bearer ".length()).trim();
            String username = userToken.split("-")[0];

            // Prüfen, ob die URL korrekt ist
            if (url.startsWith("/tradings/")) {
                String dealId = url.substring("/tradings/".length());

                // Versuche, das Handelsangebot zu löschen
                boolean success = Database.deleteTradingDeal(dealId, username);

                if (success) {
                    sendResponse(out, 200, "{\"message\":\"Trading deal deleted successfully\"}");
                } else {
                    sendResponse(out, 404, "{\"message\":\"Trading deal not found or not owned by user\"}");
                }
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
}
