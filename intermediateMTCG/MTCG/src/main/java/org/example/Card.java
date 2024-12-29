package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class Card {
    public static final String SPELL_CARD = "Spell-card";
    public static final String MONSTER_CARD = "Monster-card";

    private String name;
    private int damage;
    private String elementType;
    private String cardType;


    public Card(String name, int damage, String elementType, String cardType) {
        if (!cardType.equals(MONSTER_CARD) && !cardType.equals(SPELL_CARD)) {
            throw new IllegalArgumentException("Invalid card type: " + cardType + ". It must be either 'Spell-card' or 'Monster-card'.");
        }

        this.name = name;
        this.damage = damage;
        this.elementType = elementType;
        this.cardType = cardType;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        cardType = cardType;
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
        return elementType;
    }

    public void setElementType(String elementType) {
        elementType = elementType;
    }
}
