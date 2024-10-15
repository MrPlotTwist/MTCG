package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.User.users;
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

