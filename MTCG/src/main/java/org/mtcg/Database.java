package org.mtcg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;

public class Database {

    // Verbindungsdetails als Konstanten
    private static final String URL = "jdbc:postgresql://localhost:5432/mtcg";
    private static final String USER = "postgres";
    private static final String PASSWORD = "tomi2002";

    // Methode, um eine Verbindung zur Datenbank herzustellen
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Funktion zum Abrufen der Karten eines Benutzers
    public static List<String> getCardsForUser(String username) {
        List<String> cards = new ArrayList<>();

        // SQL-Abfrage, um die Karten eines Benutzers zu laden
        String query = """
        SELECT c.name
        FROM user_cards uc
        JOIN users u ON uc.user_id = u.id
        JOIN cards c ON uc.card_id = c.id
        WHERE u.username = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // Benutzername setzen
            stmt.setString(1, username);

            // Debugging: Zeige die ausgeführte Abfrage und den Benutzer an
            System.out.println("SQL-Abfrage wird ausgeführt für Benutzer: " + username);

            // Abfrage ausführen
            ResultSet rs = stmt.executeQuery();

            // Ergebnisse verarbeiten
            boolean hasResults = false; // Flag, um zu überprüfen, ob Ergebnisse vorhanden sind
            while (rs.next()) {
                hasResults = true;
                String cardName = rs.getString("name");
                System.out.println("Gefundene Karte: " + cardName); // Debug-Ausgabe
                cards.add(cardName);
            }

            if (!hasResults) {
                System.out.println("Keine Karten gefunden für Benutzer: " + username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cards;
    }

    public static List<String> getDeckForUser(String username) {
        List<String> deck = new ArrayList<>();

        String query = """
            SELECT c.name
            FROM user_deck ud
            JOIN users u ON ud.user_id = u.id
            JOIN cards c ON ud.card_id = c.id
            WHERE u.username = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                deck.add(rs.getString("name"));
            }

            // Wenn das Deck leer ist, Ausgabe und Rückgabe null
            if (deck.isEmpty()) {
                System.out.println("No deck configured for user: " + username);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return deck;
    }
    public static boolean configureDeck(String username, List<String> cardIds) {
        if (cardIds.size() != 4) {
            System.out.println("Ungültige Deckgröße für Benutzer: " + username);
            return false;
        }

        String checkCardOwnershipQuery = """
        SELECT COUNT(*) 
        FROM user_cards uc
        JOIN users u ON uc.user_id = u.id
        WHERE u.username = ? AND uc.card_id = ?::uuid
    """;


        String clearDeckQuery = """
        DELETE FROM user_deck
        WHERE user_id = (SELECT id FROM users WHERE username = ?)
    """;

        String insertDeckQuery = """
    INSERT INTO user_deck (user_id, card_id)
    SELECT u.id, ?::uuid
    FROM users u
    WHERE u.username = ?
    """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            System.out.println("Starte Transaktion für Benutzer: " + username);

            // Prüfen, ob alle Karten dem Benutzer gehören
            for (String cardId : cardIds) {
                try (PreparedStatement stmt = conn.prepareStatement(checkCardOwnershipQuery)) {
                    stmt.setString(1, username);
                    stmt.setString(2, cardId);

                    ResultSet rs = stmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("Karte " + cardId + " gehört nicht Benutzer: " + username);
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Altes Deck löschen
            try (PreparedStatement stmt = conn.prepareStatement(clearDeckQuery)) {
                stmt.setString(1, username);
                int rowsDeleted = stmt.executeUpdate();
                System.out.println("Gelöschte Deck-Einträge für Benutzer " + username + ": " + rowsDeleted);
            }

            // Neues Deck einfügen
            try (PreparedStatement stmt = conn.prepareStatement(insertDeckQuery)) {
                for (String cardId : cardIds) {
                    stmt.setString(1, cardId);
                    stmt.setString(2, username);
                    stmt.addBatch();
                    System.out.println("Füge Karte zum Deck hinzu: " + cardId);
                }
                int[] rowsInserted = stmt.executeBatch();
                System.out.println("Erfolgreich eingefügte Deck-Einträge: " + rowsInserted.length);
            }

            conn.commit();
            System.out.println("Transaktion erfolgreich abgeschlossen für Benutzer: " + username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static User getUserData(String username) {
        String query = "SELECT username, name, bio, image FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(username, null); // Password wird nicht zurückgegeben
                user.setName(rs.getString("name"));
                user.setBio(rs.getString("bio"));
                user.setImage(rs.getString("image"));
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateUserDetails(String username, Map<String, String> updates) {
        String query = "UPDATE users SET name = COALESCE(?, name), bio = COALESCE(?, bio), image = COALESCE(?, image) WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updates.getOrDefault("Name", null));
            stmt.setString(2, updates.getOrDefault("Bio", null));
            stmt.setString(3, updates.getOrDefault("Image", null));
            stmt.setString(4, username);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static UserStats getUserStats(String username) {
        String query = "SELECT elo, wins, losses FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int elo = rs.getInt("elo");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");

                return new UserStats(username, elo, wins, losses);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<UserStats> getScoreboard() {
        String query = "SELECT username, elo, wins, losses FROM users ORDER BY elo DESC";
        List<UserStats> scoreboard = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                int elo = rs.getInt("elo");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");

                scoreboard.add(new UserStats(username, elo, wins, losses));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scoreboard;
    }
    public static List<Card> getDeckForBattle(String username) {
        String query = """
        SELECT c.id, c.name, c.card_type, c.element_type, c.damage
        FROM user_deck ud
        JOIN cards c ON ud.card_id = c.id
        JOIN users u ON ud.user_id = u.id
        WHERE u.username = ?
    """;
        List<Card> deck = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Card card = new Card(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("card_type"),
                        rs.getString("element_type"),
                        rs.getDouble("damage")
                );
                deck.add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deck;
    }
    public static void updateELO(String username, int delta) {
        String query = """
        UPDATE users
        SET 
            elo = elo + ?,
            wins = CASE WHEN ? > 0 THEN wins + 1 ELSE wins END,
            losses = CASE WHEN ? < 0 THEN losses + 1 ELSE losses END
        WHERE username = ?
    """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, delta);        // ELO ändern
            stmt.setInt(2, delta);        // Wins inkrementieren, wenn delta > 0
            stmt.setInt(3, delta);        // Losses inkrementieren, wenn delta < 0
            stmt.setString(4, username); // Benutzername setzen
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void incrementGames(String username) {
//        String query = "UPDATE users SET games_played = games_played + 1 WHERE username = ?";
//        try (Connection conn = getConnection();
//             PreparedStatement stmt = conn.prepareStatement(query)) {
//            stmt.setString(1, username);
//            stmt.executeUpdate();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    public static String getFirstOpponent(String currentUser) {
        String query = """
        SELECT username 
        FROM battle_queue 
        WHERE username != ? 
        ORDER BY joined_at ASC 
        LIMIT 1
    """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String opponent = rs.getString("username");
                System.out.println("Opponent found for " + currentUser + ": " + opponent);
                return opponent;
            } else {
                System.out.println("No opponent available for " + currentUser + ".");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static void removeFromQueue(String username) {
        String query = "DELETE FROM battle_queue WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addToBattleQueue(String username) {
        String query = "INSERT INTO battle_queue (username, joined_at) VALUES (?, NOW())";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("User " + username + " has joined the battle queue.");
            } else {
                System.out.println("Failed to add user " + username + " to the battle queue.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean createTradingDeal(String dealId, String cardId, String type, double minDamage, String creator) {
        String query = """
        INSERT INTO trading_deals (id, card_id, type, minimum_damage, creator_id)
        VALUES (CAST(? AS UUID), CAST(? AS UUID), ?, ?, (SELECT id FROM users WHERE username = ?))
    """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dealId);
            stmt.setString(2, cardId);
            stmt.setString(3, type);
            stmt.setDouble(4, minDamage);
            stmt.setString(5, creator);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Trading> getTradingDeals() {
        String query = """
        SELECT td.id, td.card_id, td.type, td.minimum_damage, u.username AS creator
        FROM trading_deals td
        JOIN users u ON td.creator_id = u.id
    """;
        List<Trading> deals = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                deals.add(new Trading(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("card_id")),
                        rs.getString("type"),
                        rs.getDouble("minimum_damage"),
                        rs.getString("creator")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deals;
    }

}
