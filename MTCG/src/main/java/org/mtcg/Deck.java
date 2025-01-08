package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    // Karten hinzufügen
    public void addCard(Card card) {
        cards.add(card);
    }

    // Karten entfernen
    public void removeCard(Card card) {
        cards.remove(card);
    }

    // Zugriff auf die Karten
    public List<Card> getCards() {
        return cards;
    }

    @Override
    public String toString() {
        return "Deck{" +
                "cards=" + cards +
                '}';
    }
}
