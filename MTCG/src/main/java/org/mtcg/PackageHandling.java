package org.mtcg;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class PackageHandling {

    public static void handlePackageRequest(String body, OutputStream out) {
        try (Connection conn = Database.getConnection()) {
            // Neues Paket in der Datenbank erstellen
            int packageId = insertPackage(conn);

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
                insertCardAndLinkToPackage(conn, id, name, damage, packageId);

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


    private static int insertPackage(Connection conn) throws Exception {
        String insertPackageQuery = "INSERT INTO packages DEFAULT VALUES RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(insertPackageQuery)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new Exception("Fehler beim Einfügen des Pakets.");
            }
        }
    }

    private static void insertCardAndLinkToPackage(Connection conn, String id, String name, double damage, int packageId) throws Exception {
        String insertCardQuery = """
        INSERT INTO cards (id, name, damage, element_type, card_type)
        VALUES (CAST(? AS UUID), ?, ?, ?, ?)
        ON CONFLICT DO NOTHING
    """;
        String linkCardToPackageQuery = """
        INSERT INTO package_cards (package_id, card_id)
        VALUES (?, CAST(? AS UUID))
    """;

        try (PreparedStatement insertCardStmt = conn.prepareStatement(insertCardQuery);
             PreparedStatement linkCardStmt = conn.prepareStatement(linkCardToPackageQuery)) {

            // Dynamisch den Elementtyp und Kartentyp basierend auf dem Namen bestimmen
            String elementType = getElementTypeFromName(name);
            String cardType = getCardTypeFromName(name);

            // Karte in die Datenbank einfügen
            insertCardStmt.setString(1, id);
            insertCardStmt.setString(2, name);
            insertCardStmt.setDouble(3, damage);
            insertCardStmt.setString(4, elementType);
            insertCardStmt.setString(5, cardType);
            insertCardStmt.executeUpdate();

            // Karte mit dem Paket verknüpfen
            linkCardStmt.setInt(1, packageId);
            linkCardStmt.setString(2, id);
            linkCardStmt.executeUpdate();
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


    private static void sendResponse(OutputStream out, int statusCode, String body) throws Exception {
        ClientHandling.sendResponse(out, statusCode, body);
    }
}
