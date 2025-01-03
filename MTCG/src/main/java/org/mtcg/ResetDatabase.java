package org.mtcg;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetDatabase {

    public static void resetDatabase() {
        try (Connection conn = Database.getConnection()) {

            // 1. Lösche alle Einträge aus der Tabelle user_cards
            deleteAllFromTable(conn, "user_cards");

            // 2. Lösche alle Einträge aus der Tabelle package_cards
            deleteAllFromTable(conn, "package_cards");

            // 3. Lösche alle Einträge aus der Tabelle packages
            deleteAllFromTable(conn, "packages");

            // 4. Lösche alle Einträge aus der Tabelle cards
            deleteAllFromTable(conn, "cards");

            // 5. Setze Coins für alle Benutzer zurück
            resetUserCoins(conn);

            System.out.println("Datenbank wurde erfolgreich zurückgesetzt.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteAllFromTable(Connection conn, String tableName) {
        String query = "DELETE FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            int rows = stmt.executeUpdate();
            System.out.println("Tabelle " + tableName + ": " + rows + " Einträge gelöscht.");
        } catch (Exception e) {
            System.err.println("Fehler beim Löschen der Tabelle " + tableName + ": " + e.getMessage());
        }
    }

    private static void resetUserCoins(Connection conn) {
        String query = "UPDATE users SET coins = 20";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            int rows = stmt.executeUpdate();
            System.out.println("Benutzercoins zurückgesetzt für " + rows + " Benutzer.");
        } catch (Exception e) {
            System.err.println("Fehler beim Zurücksetzen der Benutzercoins: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        resetDatabase();
    }
}
