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
        if (cards.size() < 4) {
            cards.add(card);
        } else {
            throw new IllegalStateException("A deck can only contain 4 cards.");
        }
    }

    // Karten entfernen
    public void removeCard(Card card) {
        cards.remove(card);
    }

    // Deck mischen
    public void shuffle() {
        java.util.Collections.shuffle(cards);
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
