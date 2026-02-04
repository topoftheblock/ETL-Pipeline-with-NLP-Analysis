package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.config.PPRConfiguration;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.interfaces.Rede;

import java.util.*;

/**
 * Einmaliges Hilfsskript zum Bereinigen der Datenbank.
 * Entfernt doppelte Topics aus den gespeicherten Reden.
 */
public class DatabaseCleaner {

    public static void main(String[] args) {
        System.out.println("--- Starte Datenbank-Bereinigung ---");

        // 1. Verbindung aufbauen
        PPRConfiguration config = new PPRConfiguration();
        try {
            // Wir nutzen die Factory, um die konfigurierte DB (MongoDB) zu bekommen
            DatabaseConnection db = AppFactory.getInstance().createDatabaseConnection(config.getDatabasePath());

            // 2. Alle Reden laden
            List<Rede> alleReden = db.getAllReden();
            System.out.println(alleReden.size() + " Reden gefunden. Prüfe auf Duplikate...");

            int correctedCount = 0;

            for (Rede rede : alleReden) {
                List<Map<String, Object>> currentTopics = rede.getTopicStats();

                if (currentTopics != null && !currentTopics.isEmpty()) {
                    // Bereinigte Liste erstellen
                    List<Map<String, Object>> cleanTopics = removeDuplicates(currentTopics);

                    // Wenn die Größe unterschiedlich ist, gab es Duplikate -> Update DB
                    if (cleanTopics.size() < currentTopics.size()) {
                        System.out.println("Bereinige Rede " + rede.getId() + ": " + currentTopics.size() + " -> " + cleanTopics.size() + " Topics");

                        // DB Update mit den sauberen Daten
                        db.updateRedeAnalysis(
                                rede.getId(),
                                rede.getSentiment() != null ? rede.getSentiment() : 0.0,
                                cleanTopics,
                                rede.getNamedEntities(),
                                rede.getPosStats()
                        );
                        correctedCount++;
                    }
                }
            }

            System.out.println("Fertig! ");
            System.out.println(correctedCount + " Reden wurden korrigiert.");
            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hilfsmethode: Entfernt Duplikate basierend auf dem Namen
     * Behält bei Duplikaten den Eintrag mit der höheren Wahrscheinlichkeit.
     */
    private static List<Map<String, Object>> removeDuplicates(List<Map<String, Object>> rawTopics) {
        Map<String, Map<String, Object>> bestTopicMap = new HashMap<>();

        for (Map<String, Object> topic : rawTopics) {
            String name = (String) topic.get("name");
            if (name == null) continue;

            // Normalisieren: Trimmen
            String key = name.trim();

            double prob = 0.0;
            Object pObj = topic.get("probability");
            if (pObj instanceof Number) prob = ((Number) pObj).doubleValue();

            // Nur speichern, wenn wir diesen Key noch nicht haben ODER der neue Score besser ist
            if (!bestTopicMap.containsKey(key) ||
                    prob > ((Number) bestTopicMap.get(key).get("probability")).doubleValue()) {

                // Saubere Kopie erstellen mit getrimmtem Namen
                Map<String, Object> cleanEntry = new HashMap<>(topic);
                cleanEntry.put("name", key);
                bestTopicMap.put(key, cleanEntry);
            }
        }

        // Zurück zur Liste und sortieren
        List<Map<String, Object>> result = new ArrayList<>(bestTopicMap.values());
        result.sort((a, b) -> {
            Double p1 = ((Number) a.get("probability")).doubleValue();
            Double p2 = ((Number) b.get("probability")).doubleValue();
            return p2.compareTo(p1);
        });

        // Optional: Auf Top 5 begrenzen, falls gewünscht
        if (result.size() > 5) {
            return result.subList(0, 5);
        }
        return result;
    }
}