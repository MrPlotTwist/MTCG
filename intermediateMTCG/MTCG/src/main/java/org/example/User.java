package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private int coins;
    //alle Karten eines Users
    private List<Card> stack = new ArrayList<>();
    //die 4 Karten, welcher der User auswählt
    private List<Card> deck = new ArrayList<>();
    //temporäre Liste für alle User
    public static List<User> users = new ArrayList<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public static List<User> getUsers() {
        return users;
    }

    public static void setUsers(List<User> users) {
        User.users = users;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

    public List<Card> getStack() {
        return stack;
    }

    public void setStack(List<Card> stack) {
        this.stack = stack;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        if (deck.size() == 4) {
            this.deck = deck;
        } else {
            throw new IllegalArgumentException("Deck must contain exactly 4 cards.");
        }
    }
}
