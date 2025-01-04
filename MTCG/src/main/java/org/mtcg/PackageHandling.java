package org.mtcg;

import java.io.OutputStream;
import java.util.ArrayList;

import static org.mtcg.ClientHandling.sendResponse;

public class PackageHandling {

    public static void handlePackageRequest(String body, OutputStream out) {
        try {
            // Neues Paket in der Datenbank erstellen
            int packageId = Database.insertPackage();

            // Liste zur Speicherung der Karteninformationen
            ArrayList<String> cardList = new ArrayList<>();

            // Entferne die eckigen Klammern und teile den String nach "}," auf
            String[] cardDataArray = body.replace("[", "").replace("]", "").split("},");
            for (String cardData : cardDataArray) {
                if (!cardData.endsWith("}")) {
                    cardData += "}";
                }

                // Verarbeite den aktuellen Karten-String
                cardData = cardData.replaceAll("[{\"]", "").trim();
                String[] cardFields = cardData.split(",");
                String id = cardFields[0].split(":")[1].trim();
                String name = cardFields[1].split(":")[1].trim();
                String damageValue = cardFields[2].split(":")[1].trim();
                damageValue = damageValue.replace("}", "").trim();
                double damage = Double.parseDouble(damageValue);

                // Karte erstellen und in der Datenbank speichern
                Database.insertCardAndLinkToPackage(id, name, damage, packageId);

                // Karteninformationen zur Liste hinzufügen
                cardList.add(String.format("{\"Id\":\"%s\",\"Name\":\"%s\",\"Damage\":%.1f}", id, name, damage));
            }

            // JSON für das erstellte Paket und die Karten erstellen
            String jsonResponse = String.format("{\"PackageId\":%d,\"Cards\":[%s]}",
                    packageId,
                    String.join(",", cardList)
            );

            // Erfolgsausgabe
            sendResponse(out, 201, jsonResponse);
            System.out.println("Paket erfolgreich erstellt. Paket-ID: " + packageId);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(out, 500, "{\"message\":\"Internal Server Error\"}");
            } catch (Exception ioException) {
                ioException.printStackTrace();
            }
        }
    }

    static String getElementTypeFromName(String name) {
        if (name.toLowerCase().contains("water")) {
            return "water";
        } else if (name.toLowerCase().contains("fire")) {
            return "fire";
        } else {
            return "normal";
        }
    }

    static String getCardTypeFromName(String name) {
        if (name.toLowerCase().contains("goblin") || name.toLowerCase().contains("dragon") ||
                name.toLowerCase().contains("ork") || name.toLowerCase().contains("knight") ||
                name.toLowerCase().contains("kraken") || name.toLowerCase().contains("elf")) {
            return "monster";
        } else if (name.toLowerCase().contains("spell")) {
            return "spell";
        }
        return "unknown"; // Standardwert für unbekannte Kartentypen
    }
}
