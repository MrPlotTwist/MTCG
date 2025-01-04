package org.mtcg;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static org.mtcg.ClientHandling.sendResponse;

public class TransactionHandling {

    private static final int PACKAGE_COST = 5;

    public static void handleTransactionRequest(String body, OutputStream out, String username) {
        try (Connection conn = Database.getConnection()) {
            arePackagesAvailable(conn);
            if (!hasEnoughCoins(conn, username)) {
                sendResponse(out, 403, "{\"message\":\"Not enough coins\"}");
                System.out.println("NOT ENOUGH COINS");
                return;
            }

            // Wähle das älteste verfügbare Package
            int packageId = getOldestPackage(conn);
            if (packageId == -1) {
                sendResponse(out, 404, "{\"message\":\"No more packages available\"}");
                return;
            }

            // Prüfe dynamisch, ob Pakete verfügbar sind
            if (arePackagesAvailable(conn)) {
                reduceUserCoins(conn, username, out);
                // Erfolgsausgabe
                sendResponse(out, 201, "{\"message\":\"Package aquired\"}");
            }

            // Karten des Packages dem Benutzer zuweisen
            assignCardsToUser(conn, username, packageId);

            // Lösche das zugewiesene Package
            deletePackage(conn, packageId);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private static boolean hasEnoughCoins(Connection conn, String username) throws Exception {
        String query = "SELECT coins FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int coins = rs.getInt("coins");
                System.out.println("Benutzer hat " + coins + " Coins");

                if (coins >= PACKAGE_COST) {
                    return true;
                } else {
                    System.out.println("Nicht genügend Coins: " + coins + " < " + PACKAGE_COST);
                    return false;
                }
            } else {
                System.out.println("Benutzer nicht gefunden: " + username);
                return false; // Benutzer existiert nicht
            }
        }
    }

    private static void reduceUserCoins(Connection conn, String username, OutputStream out) {
        try {
            String query = "UPDATE users SET coins = coins - ? WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, PACKAGE_COST);
                stmt.setString(2, username);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("Benutzer: " + username + " hat " + PACKAGE_COST + " Coins verloren.");
                } else {
                    sendResponse(out, 404, "{\"message\":\"User not found\"}");
                }
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

    // Wählt das älteste verfügbare Paket basierend auf der niedrigsten ID
    private static int getOldestPackage(Connection conn) throws Exception {
        String query = "SELECT id FROM packages ORDER BY id ASC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int packageId = rs.getInt("id");
                System.out.println("Ältestes verfügbares Paket: " + packageId);
                return packageId;
            } else {
                return -1; // Kein Paket verfügbar
            }
        }
    }

    private static void assignCardsToUser(Connection conn, String username, int packageId) throws Exception {
        String checkQuery = "SELECT COUNT(*) FROM user_cards " +
                "WHERE user_id = (SELECT id FROM users WHERE username = ?) AND card_id = CAST(? AS UUID)";
        String assignQuery = "INSERT INTO user_cards (user_id, card_id) " +
                "SELECT (SELECT id FROM users WHERE username = ?), card_id " +
                "FROM package_cards WHERE package_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
             PreparedStatement assignStmt = conn.prepareStatement(assignQuery)) {

            // Hole alle Karten aus dem Package
            String getCardsQuery = "SELECT card_id FROM package_cards WHERE package_id = ?";
            try (PreparedStatement getCardsStmt = conn.prepareStatement(getCardsQuery)) {
                getCardsStmt.setInt(1, packageId);
                ResultSet cards = getCardsStmt.executeQuery();

                while (cards.next()) {
                    String cardId = cards.getString("card_id");

                    // Überprüfe, ob die Karte bereits dem Benutzer gehört
                    checkStmt.setString(1, username);
                    checkStmt.setString(2, cardId); // String als UUID behandeln
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("Karte " + cardId + " gehört bereits dem Benutzer: " + username);
                        continue; // Überspringe die Karte
                    }

                    // Karte dem Benutzer zuweisen
                    assignStmt.setString(1, username);
                    assignStmt.setInt(2, packageId);
                    assignStmt.executeUpdate();
                    System.out.println("Karte " + cardId + " wurde Benutzer " + username + " zugewiesen.");
                }
            }
        }
    }

    private static void deletePackage(Connection conn, int packageId) throws Exception {
        String query = "DELETE FROM packages WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, packageId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Package mit ID " + packageId + " wurde gelöscht.");
            } else {
                System.out.println("Package mit ID " + packageId + " konnte nicht gelöscht werden.");
            }
        }
    }

    private static boolean arePackagesAvailable(Connection conn) throws Exception {
        String query = "SELECT COUNT(*) AS remaining FROM packages";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) { // COUNT(*) gibt immer eine Zeile zurück
                int remaining = rs.getInt("remaining");
                if (remaining > 0) {
                    System.out.println("Verbleibende Pakete: " + remaining);
                    return true;
                } else {
                    System.out.println("No packages available");
                    return false;
                }
            }
        }
        // Der Code sollte diesen Punkt niemals erreichen, weil COUNT(*) immer eine Zeile liefert
        throw new IllegalStateException("Unexpected result from query");
    }
}
