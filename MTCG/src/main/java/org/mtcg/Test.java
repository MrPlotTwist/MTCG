package org.mtcg;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mtcg.User.users;
import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    private Server server;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        server = new Server(10001);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        server = null;
    }

    @Test
    void testUserRegistration() {
        String usernameToCheck = "otherUsername";
        String usernameToRegister = "kienboec";

        // Neuen Benutzer registrieren
        User newUser = new User(usernameToRegister, "daniel");
        users.add(newUser);

        boolean usernameExists = false;
        for (User user : users) {
            if (user.getUsername().equals(usernameToCheck)) {
                usernameExists = true;
                break;
            }
        }

        assertFalse(usernameExists, "User should NOT be registered!");

        usernameExists = false; // Zurücksetzen
        for (User user : users) {
            if (user.getUsername().equals(usernameToRegister)) {
                usernameExists = true;
                break;
            }
        }
        assertTrue(usernameExists, "User should be registered!");
    }
    @Test
    void UserLogin() {
        boolean TokenExists= false;

        // Neuen Benutzer registrieren
        User newUser = new User("kienboec", "daniel");
        users.add(newUser);
        String UserToken = newUser.getUsername() + "-mtcgToken";

        for (User user : users) {
            if ((user.getUsername() + "-mtcgToken").equals(UserToken)) {
                TokenExists = true;
                break;
            }
        }
        assertTrue(TokenExists, "User is now logged in!");
    }
}

class UserTest {

    @Test
    void testUsercreated() {
        boolean Usercreated = false;
        User newUser = new User("kienboec", "daniel");
        if(newUser.getUsername() != null && newUser.getPassword() != null) {
            Usercreated = true;
        }
        assertTrue(Usercreated, "User created!");
    }
    }

    class CardTest {
    @Test
        void testCreateCard () {
        //Karte erfolgreich erstellt
            Card newCard = new Card("1223432dfg", "Glurak", 20, "Fire", Card.MONSTER_CARD);
            assertNotNull(newCard, "Card should not be null!");

            //Karte hat falsche werte -> wird nicht erstellt
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Card("sdf", "Schiggi", 30, "Water", "Notcorrect");
        });
        assertEquals("Invalid card type: Notcorrect. It must be either 'Spell-card' or 'Monster-card'.", exception.getMessage());
        }
    }

    class PackageTest {

    @Test
    //ob alle Karten erstellt und in Liste sind
        void PackageList() {
        Card card1 = new Card("ssss", "Schiggi", 30, "Water", Card.MONSTER_CARD);
        Card card2 = new Card("lolo","Glumanda", 40, "Fire", Card.MONSTER_CARD);
        Card card3 = new Card("sdfsdfsdf","Bisasam", 35, "Grass", Card.MONSTER_CARD);
        Card card4 = new Card("sssyxcv","Pikachu", 50, "Electric", Card.MONSTER_CARD);
        Card card5 = new Card("yyy","Hydropumpe", 60, "Water", Card.SPELL_CARD);

        List<Card> packageTest = new ArrayList<>();
        packageTest.add(card1);
        packageTest.add(card2);
        packageTest.add(card3);
        packageTest.add(card4);
        packageTest.add(card5);

        Package cardPackage = new Package(packageTest);

        assertEquals(5, cardPackage.getCards().size());

        for (Card card : cardPackage.getCards()) {
            System.out.println(card.getName());
        }
    }
    @Test
    //Package Null
        void EmptyPackage() {

        Package emptyPackage = new Package();
        assertNull(emptyPackage.getCards());
    }
    }


