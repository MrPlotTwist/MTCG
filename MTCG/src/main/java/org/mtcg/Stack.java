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
