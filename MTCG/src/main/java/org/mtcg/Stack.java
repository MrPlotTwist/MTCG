package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class Stack {
    private List<Card> cards;

    public Stack() {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void removeCard(Card card) {
        cards.remove(card);
    }

    public List<Card> getCardsByElement(String element) {
        List<Card> filtered = new ArrayList<>();
        for (Card card : cards) {
            if (card.getElement().equalsIgnoreCase(element)) {
                filtered.add(card);
            }
        }
        return filtered;
    }

    public List<Card> getCardsByType(String type) {
        List<Card> filtered = new ArrayList<>();
        for (Card card : cards) {
            if (card.getType().equalsIgnoreCase(type)) {
                filtered.add(card);
            }
        }
        return filtered;
    }

    public List<Card> getAllCards() {
        return cards;
    }

    public boolean containsCard(String cardId) {
        for (Card card : cards) {
            if (card.getId().equals(cardId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Stack{" +
                "cards=" + cards +
                '}';
    }
}
