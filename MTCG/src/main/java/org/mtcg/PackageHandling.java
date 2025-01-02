package org.mtcg;

import java.io.OutputStream;
import java.util.ArrayList;

public class PackageHandling {
    public static ArrayList<Card> handlePackageRequest(String body, OutputStream out) {

        ArrayList<Card> packageOfCards = new ArrayList<>();

        // Entferne die eckigen Klammern und teile den String nach "}," auf
        String[] cardDataArray = body.replace("[", "").replace("]", "").split("},");
        for (String cardData : cardDataArray) {
            // Füge das "}" zurück, falls es entfernt wurde
            if (!cardData.endsWith("}")) {
                cardData += "}";
            }

            // Verarbeite den aktuellen Karten-String
            cardData = cardData.replaceAll("[{\"]", "").trim();
            String[] cardFields = cardData.split(",");
            String id = cardFields[0].split(":")[1].trim();
            String name = cardFields[1].split(":")[1].trim();
            String damageValue = cardFields[2].split(":")[1].trim();
// Entferne die schließende Klammer '}' falls vorhanden
            damageValue = damageValue.replace("}", "").trim();

// Konvertiere den bereinigten String in einen double
            double damage = Double.parseDouble(damageValue);

            // Karte erstellen
            Card card = new Card(id, name, damage, null, null);
            System.out.println("Karte erstellt:");
            System.out.println("Id: " + card.getId());
            System.out.println("Name: " + card.getName());
            System.out.println("Damage: " + card.getDamage());
            packageOfCards.add(card);
        }
        return packageOfCards;
    }
}
