package org.mtcg;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.List;

class MTCGTestSuite {

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
            INSERT INTO users (id, username, password) VALUES (1, 'creator', 'password');
            INSERT INTO users (id, username, password) VALUES (2, 'trader', 'password');
            INSERT INTO cards (id, name, damage, element_type, card_type)
            VALUES ('845f0dc7-37d0-426e-994e-43fc3ac83c08', 'WaterGoblin', 10.0, 'water', 'monster'),
                   ('dfdd758f-649c-40f9-ba3a-8657f4b3439f', 'FireElf', 20.0, 'fire', 'monster');
            INSERT INTO user_cards (user_id, card_id) VALUES
                (1, '845f0dc7-37d0-426e-994e-43fc3ac83c08'),
                (2, 'dfdd758f-649c-40f9-ba3a-8657f4b3439f');
            INSERT INTO trading_deals (id, card_id, type, minimum_damage, creator_id)
            VALUES ('trade-id', '845f0dc7-37d0-426e-994e-43fc3ac83c08', 'monster', 10.0, 1);
        """);
        }

        boolean result = Database.processTrade("trade-id", "trader", "dfdd758f-649c-40f9-ba3a-8657f4b3439f");
        assertTrue(result);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM user_cards WHERE card_id = ?")) {
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
