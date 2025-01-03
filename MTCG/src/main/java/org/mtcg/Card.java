package org.mtcg;

public class Card {
    private String id;
    private String name;
    private String type; // "Monster" oder "Spell"
    private String element; // "Fire", "Water", "Normal"
    private double damage;
    private String owner; // Besitzer der Karte

    public Card(String id, String name, String type, String element, double damage) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.element = element;
        this.damage = damage;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getElement() {
        return element;
    }

    public double getDamage() {
        return damage;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
    @Override
    public String toString() {
        return name + " (" + element + ", " + type + ")";
    }

}
