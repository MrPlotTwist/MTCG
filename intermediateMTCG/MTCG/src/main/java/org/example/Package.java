package org.mtcg;

import java.util.ArrayList;
import java.util.List;

public class Package {
    private List<Card> cards;

    public Package() {
        getCards();
    }

    public Package(List<Card> cards) {
        this.cards = cards;
    }

    public List<Card> getCards() {
        return cards;
    }
}
