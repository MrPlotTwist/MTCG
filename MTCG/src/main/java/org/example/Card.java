package org.example;

import java.util.ArrayList;
import java.util.List;

public class Card {
    private String name;
    private int damage;
    private String ElementType;
    private String CardType;

    public Card(String name, int damage, String elementType, String cardType) {
        this.name = name;
        this.damage = damage;
        ElementType = elementType;
        CardType = cardType;
    }

    public String getCardType() {
        return CardType;
    }

    public void setCardType(String cardType) {
        CardType = cardType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public String getElementType() {
        return ElementType;
    }

    public void setElementType(String elementType) {
        ElementType = elementType;
    }
}
