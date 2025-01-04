package org.mtcg;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

class TESTS {

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE user_deck CASCADE;");
            stmt.execute("TRUNCATE TABLE user_cards CASCADE;");
            stmt.execute("TRUNCATE TABLE users CASCADE;");
            stmt.execute("TRUNCATE TABLE cards CASCADE;");
        }
    }

    @Test
    void testRegisterNewUser() {
        boolean result = Database.registerUser("newuser", "password");
        assertTrue(result);
    }

    @Test
    void testRegisterExistingUser() {
        Database.registerUser("existinguser", "password");
        boolean result = Database.registerUser("existinguser", "password");
        assertFalse(result);
    }

    @Test
    void testRegisterUserWithoutPassword() {
        boolean result = Database.registerUser("userWithoutPassword", null);
        assertFalse(result);
    }

    @Test
    void testLoginSuccessful() {
        Database.registerUser("loginuser", "password");
        boolean result = Database.login("loginuser", "password");
        assertTrue(result);
    }

    @Test
    void testLoginWrongPassword() {
        Database.registerUser("userWrongPassword", "password");
        boolean result = Database.login("userWrongPassword", "wrongPassword");
        assertFalse(result);
    }

    @Test
    void testLoginNonexistentUser() {
        boolean result = Database.login("nonexistent", "password");
        assertFalse(result);
    }

    @Test
    void testGetElementTypeFromName() {
        assertEquals("water", PackageHandling.getElementTypeFromName("WaterGoblin"));
        assertEquals("fire", PackageHandling.getElementTypeFromName("FireSpell"));
        assertEquals("normal", PackageHandling.getElementTypeFromName("Goblin"));
    }

    @Test
    void testGetCardTypeFromName() {
        assertEquals("monster", PackageHandling.getCardTypeFromName("Goblin"));
        assertEquals("monster", PackageHandling.getCardTypeFromName("Dragon"));
        assertEquals("spell", PackageHandling.getCardTypeFromName("WaterSpell"));
        assertEquals("unknown", PackageHandling.getCardTypeFromName("UnknownCard"));
    }

    @Test
    void testGetCardsForUser() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Gültige UUIDs für die Karten verwenden
            String cardId = "845f0dc7-37d0-426e-994e-43fc3ac83c08";
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
        """);
            stmt.execute("""
            INSERT INTO users (id, username, password) 
            VALUES (1, 'testuser', 'password');
        """);
            stmt.execute("INSERT INTO user_cards (user_id, card_id) VALUES (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08');");
        }

        // Karten des Benutzers abrufen
        List<String> cards = Database.getCardsForUser("testuser");

        // Überprüfungen
        assertNotNull(cards);
        assertEquals(1, cards.size());
        assertEquals("WaterGoblin", cards.get(0));
    }


    @Test
    void testConfigureDeck() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer mit Passwort einfügen
            stmt.execute("INSERT INTO users (id, username, password) VALUES (1, 'testuser', 'password');");

            // Karten mit gültigen UUIDs hinzufügen
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
        """);
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'FireElf', 20.0, 'fire', 'monster');
        """);
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('999f0dc7-37d0-426e-994e-43fc3ac83c08', 'Dragon', 50.0, 'normal', 'monster');
        """);
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('777d758f-649c-40f9-ba3a-8657f4b3439f', 'Knight', 30.0, 'normal', 'monster');
        """);

            // Karten mit Benutzer verknüpfen
            stmt.execute("INSERT INTO user_cards (user_id, card_id) VALUES (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08');");
            stmt.execute("INSERT INTO user_cards (user_id, card_id) VALUES (1, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');");
            stmt.execute("INSERT INTO user_cards (user_id, card_id) VALUES (1, '999f0dc7-37d0-426e-994e-43fc3ac83c08');");
            stmt.execute("INSERT INTO user_cards (user_id, card_id) VALUES (1, '777d758f-649c-40f9-ba3a-8657f4b3439f');");
        }

        // Deck konfigurieren
        boolean result = Database.configureDeck("testuser", List.of(
                "845f0dc7-37d0-426e-994e-43fc3ac83c08",
                "dfdd758f-649c-40f9-ba3a-8657f4b3439f",
                "999f0dc7-37d0-426e-994e-43fc3ac83c08",
                "777d758f-649c-40f9-ba3a-8657f4b3439f"
        ));

        // Überprüfen, ob die Konfiguration erfolgreich war
        assertTrue(result);

        // Deck abrufen und überprüfen
        List<String> deck = Database.getDeckForUser("testuser");
        assertNotNull(deck);
        assertEquals(4, deck.size());
        assertTrue(deck.contains("WaterGoblin"));
        assertTrue(deck.contains("FireElf"));
        assertTrue(deck.contains("Dragon"));
        assertTrue(deck.contains("Knight"));
    }

    @Test
    void testUpdateELO() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer mit Passwort einfügen
            stmt.execute("INSERT INTO users (username, password, elo) VALUES ('testuser', 'password', 100);");
        }

        // ELO-Wert aktualisieren
        Database.updateELO("testuser", 10);

        // Überprüfen, ob der ELO-Wert korrekt aktualisiert wurde
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT elo FROM users WHERE username = ?")) {
            stmt.setString(1, "testuser");
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(110, rs.getInt("elo"));
        }
    }

    @Test
    void testCreateTradingDeal() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
            INSERT INTO users (id, username, password) VALUES (1, 'creator', 'password');
            INSERT INTO cards (id, name, damage, element_type, card_type)
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
        """);
        }

        boolean result = Database.createTradingDeal(
                "dfdd758f-649c-40f9-ba3a-8657f4b3439f", // Gültige UUID
                "845f0dc7-37d0-426e-994e-43fc3ac83c08", // Gültige UUID
                "monster",
                10.0,
                "creator"
        );

        assertTrue(result);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM trading_deals WHERE id = CAST(? AS UUID)")) {
            stmt.setString(1, "dfdd758f-649c-40f9-ba3a-8657f4b3439f"); // Gültige UUID
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("845f0dc7-37d0-426e-994e-43fc3ac83c08", rs.getString("card_id"));
        }
    }


    @Test
    void testGetTradingDeals() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
            INSERT INTO users (id, username, password) VALUES (1, 'creator', 'password');
            INSERT INTO cards (id, name, damage, element_type, card_type)
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
            INSERT INTO trading_deals (id, card_id, type, minimum_damage, creator_id)
            VALUES ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', '845f0dc7-37d0-426e-994e-43fc3ac83c08', 'monster', 10.0, 1);
        """);
        }

        List<Trading> deals = Database.getTradingDeals();

        assertNotNull(deals);
        assertEquals(1, deals.size());
        Trading deal = deals.get(0);
        assertEquals("dfdd758f-649c-40f9-ba3a-8657f4b3439f", deal.getId().toString());
        //assertEquals("WaterGoblin", deal.getCardId().toString());
        assertEquals("monster", deal.getType());
    }

    @Test
    void testDeleteTradingDeal() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
            INSERT INTO users (id, username, password) VALUES (1, 'creator', 'password');
            INSERT INTO cards (id, name, damage, element_type, card_type)
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
            INSERT INTO trading_deals (id, card_id, type, minimum_damage, creator_id)
            VALUES ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', '845f0dc7-37d0-426e-994e-43fc3ac83c08', 'monster', 10.0, 1);
        """);
        }

        // Aufruf der Methode zur Löschung des Handelsdeals
        boolean result = Database.deleteTradingDeal("dfdd758f-649c-40f9-ba3a-8657f4b3439f", "creator");
        assertTrue(result);

        // Verifizierung, dass der Handelsdeal gelöscht wurde
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM trading_deals WHERE id = CAST(? AS UUID)")) { // CAST hinzugefügt
            stmt.setString(1, "dfdd758f-649c-40f9-ba3a-8657f4b3439f");
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next());
        }
    }


    @Test
    void testProcessTrade() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
            INSERT INTO users (id, username, password) VALUES 
                (1, 'creator', 'password'),
                (2, 'trader', 'password');
            INSERT INTO cards (id, name, damage, element_type, card_type)
            VALUES 
                ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster'),
                ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'FireElf', 20.0, 'fire', 'monster');
            INSERT INTO user_cards (user_id, card_id) VALUES
                (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08'),
                (2, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');
            INSERT INTO trading_deals (id, card_id, type, minimum_damage, creator_id)
            VALUES ('123e4567-e89b-12d3-a456-426614174000', '845f0dc7-37d0-426e-994e-43fc3ac83c08', 'monster', 10.0, 1);
        """);
        }

        // Process trade
        boolean result = Database.processTrade("123e4567-e89b-12d3-a456-426614174000", "trader", "dfdd758f-649c-40f9-ba3a-8657f4b3439f");
        assertTrue(result);

        // Verify card ownership
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM user_cards WHERE card_id = CAST(? AS UUID)")) {
            stmt.setString(1, "845f0dc7-37d0-426e-994e-43fc3ac83c08");
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt("user_id"));

            stmt.setString(1, "dfdd758f-649c-40f9-ba3a-8657f4b3439f");
            rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("user_id"));
        }
    }

    @Test
    void testGetUserId() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer hinzufügen
            stmt.execute("INSERT INTO users (id, username, password) VALUES (1, 'testuser', 'password');");
        }

        try {
            // Benutzer-ID abrufen
            int userId = Database.getUserId("testuser");

            // Überprüfen, ob die richtige ID zurückgegeben wird
            assertEquals(1, userId);
        } catch (Exception e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }

        try {
            // Abrufen einer nicht vorhandenen Benutzer-ID
            Database.getUserId("nonexistentuser");
            fail("Expected an exception for a non-existent user");
        } catch (Exception e) {
            assertEquals("User not found: nonexistentuser", e.getMessage());
        }
    }

    @Test
    void testInsertPackage() throws Exception {
        int packageId = Database.insertPackage();
        assertTrue(packageId > 0, "Package ID should be greater than 0");

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id FROM packages WHERE id = " + packageId)) {
            assertTrue(rs.next(), "Inserted package should exist in the database");
        }
    }

    @Test
    void testInsertCardAndLinkToPackage() throws Exception {
        int packageId = Database.insertPackage();
        Database.insertCardAndLinkToPackage(
                "845f0dc7-37d0-426e-994e-43fc3ac83c08",
                "WaterGoblin",
                10.0,
                packageId
        );

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT card_id FROM package_cards WHERE package_id = " + packageId)) {
            assertTrue(rs.next(), "Inserted card should be linked to the package");
            assertEquals("845f0dc7-37d0-426e-994e-43fc3ac83c08", rs.getString("card_id"), "Card ID should match");
        }
    }

    @Test
    void testArePackagesAvailable() throws Exception {
        assertFalse(Database.arePackagesAvailable(), "No packages should be available initially");
        Database.insertPackage();
        assertTrue(Database.arePackagesAvailable(), "Packages should be available after insertion");
    }

    @Test
    void testHasEnoughCoins() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
            INSERT INTO users (username, password, coins) 
            VALUES ('testuser', 'password', 10);
        """);
        }
        assertTrue(Database.hasEnoughCoins("testuser", 5), "User should have enough coins");
        assertFalse(Database.hasEnoughCoins("testuser", 15), "User should not have enough coins");
    }


    @Test
    void testGetOldestPackage() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Lösche alle vorhandenen Pakete, um sicherzustellen, dass die Testumgebung sauber ist
            stmt.execute("DELETE FROM packages");

            // Zwei Pakete einfügen
            int package1 = Database.insertPackage();
            int package2 = Database.insertPackage();

            // Ältestes Paket abrufen
            int oldestPackage = Database.getOldestPackage();

            // Validieren, dass das älteste Paket die erste eingefügte ID ist
            assertEquals(package1, oldestPackage, "Oldest package ID should match the first inserted package");
        }
    }


    @Test
    void testReduceUserCoins() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer mit einem gültigen Passwort einfügen
            stmt.execute("INSERT INTO users (username, password, coins) VALUES ('testuser', 'password', 10);");
        }

        // Coins reduzieren
        Database.reduceUserCoins("testuser", 5);

        // Verifizieren, dass die Coins korrekt aktualisiert wurden
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT coins FROM users WHERE username = ?")) {
            stmt.setString(1, "testuser");
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "User should exist");
            assertEquals(5, rs.getInt("coins"), "User's coins should be reduced by 5");
        }
    }


    @Test
    void testAssignCardsToUser() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer mit Passwort einfügen
            stmt.execute("INSERT INTO users (id, username, password) VALUES (1, 'testuser', 'password');");

            // Karte und Paket vorbereiten
            stmt.execute("""
            INSERT INTO cards (id, name, damage, element_type, card_type) 
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster');
        """);
            stmt.execute("INSERT INTO packages (id) VALUES (1);");
            stmt.execute("""
            INSERT INTO package_cards (package_id, card_id) 
            VALUES (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08');
        """);
        }

        // Methode testen
        Database.assignCardsToUser("testuser", 1);

        // Überprüfen, ob die Karte dem Benutzer zugewiesen wurde
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT card_id FROM user_cards WHERE user_id = 1");
             ResultSet rs = stmt.executeQuery()) {
            assertTrue(rs.next(), "Card should be assigned to the user");
            assertEquals("845f0dc7-37d0-426e-994e-43fc3ac83c08", rs.getString("card_id"), "Card ID should match");
        }
    }


    @Test
    void testDeletePackage() throws Exception {
        int packageId = Database.insertPackage();
        Database.deletePackage(packageId);

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT id FROM packages WHERE id = " + packageId)) {
            assertFalse(rs.next(), "Deleted package should not exist in the database");
        }
    }


    @Test
    void testBattleWithDraw() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer und Karten einfügen
            stmt.execute("INSERT INTO users (id, username, password) VALUES (1, 'player1', 'password'), (2, 'player2', 'password');");
            stmt.execute("""
                INSERT INTO cards (id, name, damage, element_type, card_type) VALUES
                ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'NormalMonster1', 10.0, 'normal', 'monster'),
                ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'NormalMonster2', 10.0, 'normal', 'monster');
            """);
            stmt.execute("""
                INSERT INTO user_deck (user_id, card_id) VALUES
                (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08'),
                (2, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');
            """);
        }

        BattleResult result = BattleHandling.startBattle("player1", "player2");

        assertTrue(result.getLog().stream().anyMatch(log -> log.contains("The battle ends in a draw.")));
    }

    @Test
    void testElementMultiplierWaterVsFire() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer und Karten einfügen
            stmt.execute("INSERT INTO users (id, username, password) VALUES (1, 'player1', 'password'), (2, 'player2', 'password');");
            stmt.execute("""
                INSERT INTO cards (id, name, damage, element_type, card_type) VALUES
                ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterSpell', 10.0, 'water', 'spell'),
                ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'FireMonster', 5.0, 'fire', 'monster');
            """);
            stmt.execute("""
                INSERT INTO user_deck (user_id, card_id) VALUES
                (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08'),
                (2, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');
            """);
        }

        BattleResult result = BattleHandling.startBattle("player1", "player2");

        assertTrue(result.getLog().stream().anyMatch(log -> log.contains("WaterSpell deals 10.0 damage")));
        assertTrue(result.getLog().stream().anyMatch(log -> log.contains("player1 wins this round.")));
    }

    @Test
    void testStatUpdatesAfterWin() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            // Benutzer und Karten einfügen
            stmt.execute("INSERT INTO users (id, username, password, elo) VALUES (1, 'winner', 'password', 100), (2, 'loser', 'password', 100);");
            stmt.execute("""
                INSERT INTO cards (id, name, damage, element_type, card_type) VALUES
                ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'StrongMonster', 50.0, 'normal', 'monster'),
                ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'WeakMonster', 10.0, 'normal', 'monster');
            """);
            stmt.execute("""
                INSERT INTO user_deck (user_id, card_id) VALUES
                (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08'),
                (2, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');
            """);
        }

        BattleResult result = BattleHandling.startBattle("winner", "loser");

        assertTrue(result.getLog().stream().anyMatch(log -> log.contains("Winner: winner")));


        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT elo FROM users WHERE username = ?")) {
            stmt.setString(1, "winner");
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(103, rs.getInt("elo"));

            stmt.setString(1, "loser");
            rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(95, rs.getInt("elo"));
        }
    }


    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE user_deck CASCADE;");
            stmt.execute("TRUNCATE TABLE user_cards CASCADE;");
            stmt.execute("TRUNCATE TABLE users CASCADE;");
            stmt.execute("TRUNCATE TABLE cards CASCADE;");
        }
    }

}
