package org.mtcg;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetDatabase {

    public static void resetDatabase() {
        try (Connection conn = Database.getConnection()) {

            // 1. Lösche alle Einträge aus den Tabellen
            deleteAllFromTable(conn, "battles");
            deleteAllFromTable(conn, "user_cards");
            deleteAllFromTable(conn, "package_cards");
            deleteAllFromTable(conn, "packages");
            deleteAllFromTable(conn, "cards");
            deleteAllFromTable(conn, "users");

            // 2. Setze alle Sequenzen zurück
            resetSequence(conn, "battles_id_seq");
            resetSequence(conn, "packages_id_seq");
            resetSequence(conn, "users_id_seq");

            // 3. Setze Coins für alle Benutzer zurück
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

    private static void resetSequence(Connection conn, String sequenceName) {
        String query = "ALTER SEQUENCE " + sequenceName + " RESTART WITH 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.executeUpdate();
            System.out.println("Sequenz " + sequenceName + " wurde zurückgesetzt.");
        } catch (Exception e) {
            System.err.println("Fehler beim Zurücksetzen der Sequenz " + sequenceName + ": " + e.getMessage());
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
