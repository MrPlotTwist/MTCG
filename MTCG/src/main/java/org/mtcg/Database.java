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
    private static final String TEST_URL = "jdbc:postgresql://localhost:5432/mtcg_test";
    private static final String USER = "postgres";
    private static final String PASSWORD = "tomi2002";
    private static String currentDbUrl = URL;

    public static void useTestDatabase() {
        currentDbUrl = TEST_URL;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(currentDbUrl, USER, PASSWORD);
    }

    public static boolean login(String username, String password) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Login successful for user: " + username);
                return true; // Benutzer existiert mit korrekten Anmeldedaten
            }
        } catch (Exception e) {
            System.err.println("Error during login process: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Login failed for user: " + username);
        return false; // Benutzer existiert nicht oder Passwort ist falsch
    }

    public static boolean registerUser(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Username or password is empty.");
            return false;
        }

        try (Connection conn = Database.getConnection()) {
            // Check if user already exists
            String checkUserQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("User already exists: " + username);
                    return false; // User already exists
                }
            }

            // Insert new user
            String insertUserQuery = "INSERT INTO users (username, password, coins) VALUES (?, ?, 20)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.executeUpdate();
                System.out.println("User registered successfully: " + username);
                return true; // User successfully registered
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Internal error
        }
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

    public static boolean deleteTradingDeal(String dealId, String username) {
        String query = """
        DELETE FROM trading_deals
        WHERE id = CAST(? AS UUID)
          AND creator_id = (SELECT id FROM users WHERE username = ?)
    """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dealId);
            stmt.setString(2, username);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean processTrade(String dealId, String username, String offeredCardId) {
        String getDealQuery = """
    SELECT card_id, type, minimum_damage, creator_id 
    FROM trading_deals 
    WHERE id = CAST(? AS UUID)
    """;

        String validateCardQuery = """
    SELECT id 
    FROM cards 
    WHERE id = CAST(? AS UUID) AND card_type = ? AND damage >= ?
    AND id IN (SELECT card_id FROM user_cards WHERE user_id = (SELECT id FROM users WHERE username = ?))
    """;

        String updateOwnershipQuery = """
    UPDATE user_cards 
    SET user_id = ? 
    WHERE card_id = CAST(? AS UUID)
    """;

        String deleteDealQuery = "DELETE FROM trading_deals WHERE id = CAST(? AS UUID)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            System.out.println("Starting trade process...");
            System.out.println("Deal ID: " + dealId + ", Username: " + username + ", Offered Card ID: " + offeredCardId);

            // 1. Handelsangebot abrufen
            try (PreparedStatement getDealStmt = conn.prepareStatement(getDealQuery)) {
                getDealStmt.setString(1, dealId);
                ResultSet dealRs = getDealStmt.executeQuery();

                if (!dealRs.next()) {
                    System.out.println("No trade deal found for ID: " + dealId);
                    return false; // Kein Handelsangebot gefunden
                }

                String cardToTrade = dealRs.getString("card_id");
                String type = dealRs.getString("type");
                double minDamage = dealRs.getDouble("minimum_damage");
                int creatorId = dealRs.getInt("creator_id");

                System.out.println("Trade deal details - Card to Trade: " + cardToTrade + ", Type: " + type +
                        ", Min Damage: " + minDamage + ", Creator ID: " + creatorId);

                // 2. Überprüfen, ob die angebotene Karte gültig ist
                System.out.println("Validating offered card: " + offeredCardId);
                try (PreparedStatement validateCardStmt = conn.prepareStatement(validateCardQuery)) {
                    validateCardStmt.setString(1, offeredCardId);
                    validateCardStmt.setString(2, type);
                    validateCardStmt.setDouble(3, minDamage);
                    validateCardStmt.setString(4, username);

                    ResultSet validateRs = validateCardStmt.executeQuery();
                    if (!validateRs.next()) {
                        System.out.println("Offered card validation failed.");
                        conn.rollback();
                        return false; // Karte nicht gültig
                    }
                }

                // 2. Überprüfen, ob der Benutzer mit sich selbst handelt
                String checkUserQuery = "SELECT id FROM users WHERE username = ?";
                try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserQuery)) {
                    checkUserStmt.setString(1, username);
                    ResultSet userRs = checkUserStmt.executeQuery();

                    if (userRs.next()) {
                        int userId = userRs.getInt("id");
                        if (userId == creatorId) {
                            System.out.println("User " + username + " tried to trade with themselves.");
                            conn.rollback();
                            return false; // Benutzer handelt mit sich selbst
                        }
                    } else {
                        System.out.println("User " + username + " does not exist.");
                        conn.rollback();
                        return false; // Benutzer existiert nicht
                    }
                }

                // 3. Besitz der Karten tauschen
                System.out.println("Updating ownership for cards...");
                try (PreparedStatement updateStmt = conn.prepareStatement(updateOwnershipQuery)) {
                    // Besitzer der Karte im Deal ändern
                    updateStmt.setInt(1, getUserId(username));
                    updateStmt.setString(2, cardToTrade);
                    int updatedRows = updateStmt.executeUpdate();
                    System.out.println("Updated ownership for card_to_trade. Rows affected: " + updatedRows);
                    if (updatedRows == 0) {
                        System.out.println("Failed to update ownership for card_to_trade.");
                        conn.rollback();
                        return false;
                    }

                    // Besitzer der angebotenen Karte ändern
                    updateStmt.setInt(1, creatorId);
                    updateStmt.setString(2, offeredCardId);
                    updatedRows = updateStmt.executeUpdate();
                    System.out.println("Updated ownership for offered card. Rows affected: " + updatedRows);
                    if (updatedRows == 0) {
                        System.out.println("Failed to update ownership for offered card.");
                        conn.rollback();
                        return false;
                    }
                }

                // 4. Handelsangebot löschen
                System.out.println("Deleting trade deal: " + dealId);
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteDealQuery)) {
                    deleteStmt.setString(1, dealId);
                    int deletedRows = deleteStmt.executeUpdate();
                    System.out.println("Deleted trade deal. Rows affected: " + deletedRows);
                }

                conn.commit();
                System.out.println("Trade process completed successfully.");
                return true;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static int getUserId(String username) throws Exception {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new Exception("User not found: " + username);
            }
        }
    }

    public static int insertPackage() throws Exception {
        String insertPackageQuery = "INSERT INTO packages DEFAULT VALUES RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertPackageQuery);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new Exception("Fehler beim Einfügen des Pakets.");
            }
        }
    }

    public static void insertCardAndLinkToPackage(String id, String name, double damage, int packageId) throws Exception {
        String insertCardQuery = """
    INSERT INTO cards (id, name, damage, element_type, card_type)
    VALUES (CAST(? AS UUID), ?, ?, ?, ?)
    ON CONFLICT DO NOTHING
    """;
        String linkCardToPackageQuery = """
    INSERT INTO package_cards (package_id, card_id)
    VALUES (?, CAST(? AS UUID))
    """;

        try (Connection conn = getConnection();
             PreparedStatement insertCardStmt = conn.prepareStatement(insertCardQuery);
             PreparedStatement linkCardStmt = conn.prepareStatement(linkCardToPackageQuery)) {

            String elementType = PackageHandling.getElementTypeFromName(name);
            String cardType = PackageHandling.getCardTypeFromName(name);

            insertCardStmt.setString(1, id);
            insertCardStmt.setString(2, name);
            insertCardStmt.setDouble(3, damage);
            insertCardStmt.setString(4, elementType);
            insertCardStmt.setString(5, cardType);
            insertCardStmt.executeUpdate();

            linkCardStmt.setInt(1, packageId);
            linkCardStmt.setString(2, id);
            linkCardStmt.executeUpdate();
        }
    }

    public static boolean arePackagesAvailable() throws Exception {
        String query = "SELECT COUNT(*) AS remaining FROM packages";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() && rs.getInt("remaining") > 0;
        }
    }

    public static boolean hasEnoughCoins(String username, int cost) throws Exception {
        String query = "SELECT coins FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("coins") >= cost;
        }
    }

    public static int getOldestPackage() throws Exception {
        String query = "SELECT id FROM packages ORDER BY id ASC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        }
    }

    public static void reduceUserCoins(String username, int cost) throws Exception {
        String query = "UPDATE users SET coins = coins - ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, cost);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    public static void assignCardsToUser(String username, int packageId) throws Exception {
        String assignQuery = """
    INSERT INTO user_cards (user_id, card_id)
    SELECT (SELECT id FROM users WHERE username = ?), card_id
    FROM package_cards WHERE package_id = ?
    """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(assignQuery)) {
            stmt.setString(1, username);
            stmt.setInt(2, packageId);
            stmt.executeUpdate();
        }
    }

    public static void deletePackage(int packageId) throws Exception {
        String query = "DELETE FROM packages WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, packageId);
            stmt.executeUpdate();
        }
    }

    public static boolean isBattleActive(String player1, String player2) throws SQLException {
        String query = "SELECT 1 FROM active_battles WHERE (player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, player1);
            stmt.setString(2, player2);
            stmt.setString(3, player2);
            stmt.setString(4, player1);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static void markBattleAsActive(String player1, String player2) throws SQLException {
        String query = "INSERT INTO active_battles (player1, player2) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, player1);
            stmt.setString(2, player2);
            stmt.executeUpdate();
        }
    }

    public static void removeBattleMarker(String player1, String player2) throws SQLException {
        String query = "DELETE FROM active_battles WHERE (player1 = ? AND player2 = ?) OR (player1 = ? AND player2 = ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, player1);
            stmt.setString(2, player2);
            stmt.setString(3, player2);
            stmt.setString(4, player1);
            stmt.executeUpdate();
        }
    }





}
