package org.mtcg;

import java.util.UUID;

public class Trading {
    private UUID id;
    private UUID cardToTrade;
    private String type;
    private double minimumDamage;
    private String creator;

    public Trading(UUID id, UUID cardToTrade, String type, double minimumDamage, String creator) {
        this.id = id;
        this.cardToTrade = cardToTrade;
        this.type = type;
        this.minimumDamage = minimumDamage;
        this.creator = creator;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardToTrade() {
        return cardToTrade;
    }

    public String getType() {
        return type;
    }

    public double getMinimumDamage() {
        return minimumDamage;
    }

    public String getCreator() {
        return creator;
    }
}
