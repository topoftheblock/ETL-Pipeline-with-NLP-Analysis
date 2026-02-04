package org.texttechnologylab.ppr;

import org.bson.Document;
import com.mongodb.client.*;
import java.util.*;

/**
 * Kleines Test-Programm um schnell zu prüfen, ob die MongoDB-Daten da sind
 * und ob die NLP-Felder (Sentiment, POS, Topics) wirklich gespeichert wurden.
 * Idee:
 * - Verbindung zu MongoDB aufbauen
 * - Collection "reden" prüfen
 * - Eine Rede ausgeben und ein paar wichtige Felder anzeigen
 * @author Christian Block und Hamed Noori
 */

public class CheckDB {
    public static void main(String[] args) {
        // Verbindungseinstellungen (hartcodiert für Test)
        String connectionString = "mongodb://localhost:27017";
        String dbName = "bundestag_db";

        System.out.println("--- DIAGNOSE START ---");
        try (MongoClient client = MongoClients.create(connectionString)) {
            MongoDatabase db = client.getDatabase(dbName);
            MongoCollection<Document> reden = db.getCollection("reden");

            long count = reden.countDocuments();
            System.out.println("Anzahl Reden in DB: " + count);

            if (count > 0) {
                // Erste Rede holen
                Document doc = reden.find().first();
                System.out.println("Prüfe Rede ID: " + doc.getString("id"));

                // 1. Sentiment prüfen
                Object sentiment = doc.get("sentiment");
                System.out.println(" -> Sentiment: " + sentiment + " (Typ: " + (sentiment != null ? sentiment.getClass().getSimpleName() : "null") + ")");

                // 2. POS Stats prüfen
                Object pos = doc.get("posStats");
                System.out.println(" -> POS Stats: " + pos);
                if (pos instanceof Document) {
                    System.out.println("    (Enthält " + ((Document)pos).size() + " Einträge)");
                }

                // 3. Topics prüfen
                Object topics = doc.get("topicStats");
                System.out.println(" -> Topic Stats: " + topics);
                if (topics instanceof List) {
                    System.out.println("    (Enthält " + ((List)topics).size() + " Einträge)");
                }

            } else {
                System.err.println("KEINE Reden in der Datenbank gefunden!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--- DIAGNOSE ENDE ---");
    }
}