package org.mtcg;

import java.io.OutputStream;
import static org.mtcg.ClientHandling.sendResponse;

public class TransactionHandling {

    private static final int PACKAGE_COST = 5;

    public static void handleTransactionRequest(String body, OutputStream out, String username) {
        try {
            // Prüfen, ob Pakete verfügbar sind
            if (!Database.arePackagesAvailable()) {
                sendResponse(out, 404, "{\"message\":\"No more packages available\"}");
                return;
            }

            // Prüfen, ob der Benutzer genügend Coins hat
            if (!Database.hasEnoughCoins(username, PACKAGE_COST)) {
                sendResponse(out, 403, "{\"message\":\"Not enough money\"}");
                return;
            }

            // Wähle das älteste verfügbare Paket
            int packageId = Database.getOldestPackage();
            if (packageId == -1) {
                sendResponse(out, 404, "{\"message\":\"No packages available\"}");
                return;
            }

            // Coins des Benutzers reduzieren
            Database.reduceUserCoins(username, PACKAGE_COST);

            // Karten des Pakets dem Benutzer zuweisen
            Database.assignCardsToUser(username, packageId);

            // Paket löschen
            Database.deletePackage(packageId);

            // Erfolgsausgabe
            sendResponse(out, 201, "{\"message\":\"Package acquired\"}");
            System.out.println("Package successfully acquired by " + username);

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
