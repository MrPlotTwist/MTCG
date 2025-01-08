package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private int coins;
    //private List<Card> stack = new ArrayList<>();
    //private List<Card> deck = new ArrayList<>();

    private String name;
    private String bio;
    private String image;

    // Konstruktor
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.coins = 20; // Standardwert
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

//    public List<Card> getStack() {
//        return stack;
//    }

//    public void setStack(List<Card> stack) {
//        this.stack = stack;
//    }
//
//    public List<Card> getDeck() {
//        return deck;
//    }

    // toString-Methode erweitern
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", coins=" + coins +
                ", name='" + name + '\'' +
                ", bio='" + bio + '\'' +
                ", image='" + image + '\'' +
                '}';
    }
}
