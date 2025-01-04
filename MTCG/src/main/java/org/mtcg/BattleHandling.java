package org.mtcg;

import java.util.*;

public class BattleHandling {

    private static final int MAX_ROUNDS = 100;

    public static BattleResult startBattle(String player1, String player2) {
        List<Card> deck1 = Database.getDeckForBattle(player1);
        List<Card> deck2 = Database.getDeckForBattle(player2);

        if (deck1.isEmpty() || deck2.isEmpty()) {
            return new BattleResult(false, "One or both players do not have a configured deck.");
        }

        int rounds = 0;
        int player1Wins = 0;
        int player2Wins = 0;
        boolean player1UsedBooster = false;
        boolean player2UsedBooster = false;
        List<String> log = new ArrayList<>();

        log.add("Starting battle between " + player1 + " and " + player2);

        while (!deck1.isEmpty() && !deck2.isEmpty() && rounds < MAX_ROUNDS) {
            rounds++;
            log.add("Round " + rounds);

            // Karten zufällig auswählen
            Card card1 = deck1.get((int) (Math.random() * deck1.size()));
            Card card2 = deck2.get((int) (Math.random() * deck2.size()));

            log.add(player1 + " plays: " + card1);
            log.add(player2 + " plays: " + card2);

            // Spezialfälle überprüfen
            String specialCaseResult1 = checkSpecialCases(card1, card2);
            String specialCaseResult2 = checkSpecialCases(card2, card1);

            // Ergebnisse der Spezialfälle behandeln
            if (specialCaseResult1 != null) {
                log.add(specialCaseResult1);
                if (specialCaseResult1.contains(player1)) {
                    player1Wins++;
                    deck2.remove(card2);
                    deck1.add(card2);
                } else if (specialCaseResult1.contains(player2)) {
                    player2Wins++;
                    deck1.remove(card1);
                    deck2.add(card1);
                }
                continue; // Nächste Runde starten
            }

            if (specialCaseResult2 != null) {
                log.add(specialCaseResult2);
                if (specialCaseResult2.contains(player1)) {
                    player1Wins++;
                    deck2.remove(card2);
                    deck1.add(card2);
                } else if (specialCaseResult2.contains(player2)) {
                    player2Wins++;
                    deck1.remove(card1);
                    deck2.add(card1);
                }
                continue; // Nächste Runde starten
            }

            // Booster-Logik
            boolean player1Booster = false;
            boolean player2Booster = false;

            if (!player1UsedBooster && Math.random() > 0.7) { // 30% Chance, den Booster einzusetzen
                player1Booster = true;
                player1UsedBooster = true;
                log.add(player1 + " uses a damage booster on " + card1.getName() + "!");
            }

            if (!player2UsedBooster && Math.random() > 0.7) { // 30% Chance, den Booster einzusetzen
                player2Booster = true;
                player2UsedBooster = true;
                log.add(player2 + " uses a damage booster on " + card2.getName() + "!");
            }

            // Schaden berechnen
            double damage1 = calculateDamage(card1, card2, player1Booster);
            double damage2 = calculateDamage(card2, card1, player2Booster);

            log.add(card1.getName() + " deals " + damage1 + " damage");
            log.add(card2.getName() + " deals " + damage2 + " damage");

            if (damage1 > damage2) {
                log.add(player1 + " wins this round.");
                player1Wins++;
                deck2.remove(card2);
                deck1.add(card2);
            } else if (damage2 > damage1) {
                log.add(player2 + " wins this round.");
                player2Wins++;
                deck1.remove(card1);
                deck2.add(card1);
            } else {
                log.add("Round ends in a draw.");
            }
        }

        // Endergebnis
        log.add("The battle ends after " + rounds + " rounds.");
        log.add(player1 + " won " + player1Wins + " rounds.");
        log.add(player2 + " won " + player2Wins + " rounds.");

        boolean hasWinner = false;
        String winner = null;
        String loser = null;

        if (player1Wins > player2Wins) {
            log.add("Winner: " + player1);
            winner = player1;
            loser = player2;
            hasWinner = true;
        } else if (player2Wins > player1Wins) {
            log.add("Winner: " + player2);
            winner = player2;
            loser = player1;
            hasWinner = true;
        } else {
            log.add("The battle ends in a draw.");
        }

        // Spielerstatistiken aktualisieren
        updateStats(winner, loser, hasWinner);

        return new BattleResult(true, log.get(log.size() - 1), log);
    }

    private static double calculateDamage(Card attacker, Card defender, boolean applyBooster) {
        double baseDamage = attacker.getDamage();
        if (applyBooster) {
            baseDamage *= 1.5; // 50% mehr Schaden durch Booster
        }

        if (attacker.getType().equals("Spell") || defender.getType().equals("Spell")) {
            double multiplier = getElementMultiplier(attacker.getElement(), defender.getElement());
            return baseDamage * multiplier;
        }

        return baseDamage;
    }



    private static String checkSpecialCases(Card card1, Card card2) {
        if (card1.getName().contains("Goblin") && card2.getName().contains("Dragon")) {
            return "Goblin is too afraid to attack Dragon. " + card2.getOwner() + " wins this round.";
        }
        if (card1.getName().contains("Knight") && card2.getName().contains("WaterSpell")) {
            return "Knight drowns due to WaterSpell. " + card2.getOwner() + " wins this round.";
        }
        if (card1.getName().contains("Wizzard") && card2.getName().contains("Ork")) {
            return "Wizzard controls the Ork. " + card1.getOwner() + " wins this round.";
        }
        if (card1.getName().contains("Kraken") && card2.getType().equals("Spell")) {
            return "Kraken is immune to spells. " + card1.getOwner() + " wins this round.";
        }
        if (card1.getName().contains("FireElf") && card2.getName().contains("Dragon")) {
            return "FireElf evades Dragon's attack. " + card1.getOwner() + " wins this round.";
        }
        return null;
    }

    private static double calculateDamage(Card attacker, Card defender) {
        if (attacker.getType().equals("Spell") || defender.getType().equals("Spell")) {
            // Element-basierte Effizienz
            double multiplier = getElementMultiplier(attacker.getElement(), defender.getElement());
            return attacker.getDamage() * multiplier;
        }
        return attacker.getDamage(); // Monster-Kampf: Schaden unverändert
    }

    private static double getElementMultiplier(String attackerElement, String defenderElement) {
        if (attackerElement.equals("Water") && defenderElement.equals("Fire")) return 2.0;
        if (attackerElement.equals("Fire") && defenderElement.equals("Normal")) return 2.0;
        if (attackerElement.equals("Normal") && defenderElement.equals("Water")) return 2.0;
        if (defenderElement.equals("Water") && attackerElement.equals("Fire")) return 0.5;
        if (defenderElement.equals("Fire") && attackerElement.equals("Normal")) return 0.5;
        if (defenderElement.equals("Normal") && attackerElement.equals("Water")) return 0.5;
        return 1.0; // Kein Effekt
    }

    private static void updateStats(String winner, String loser, boolean hasWinner) {
        if (hasWinner) {
            Database.updateELO(winner, 3);
            Database.updateELO(loser, -5);
        }
    }
}
