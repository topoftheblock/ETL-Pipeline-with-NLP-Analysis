package org.texttechnologylab.ppr.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Kapselt die Verbindungsdaten zur MongoDB.
 * Aktualisiert auf MongoDB Driver 5.x (Sync).
 */
public class MongoConnector {

    private String user = "PPR_2025_227_rw";
    private String password = "sHPqeaJY";
    private String host = "ppr.lehre.texttechnologylab.org";
    private int port = 27020;
    private String databaseName = "PPR_2025_227";
    private String authSource = "PPR_2025_227";

    public MongoConnector() {
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("ppr.properties")) {
            if (input != null) {
                Properties props = new Properties();
                try {
                    props.load(input);
                } catch (IllegalArgumentException e) {
                    input.reset();
                    props.loadFromXML(input);
                }

                if (props.getProperty("mongodb.user") != null) this.user = props.getProperty("mongodb.user");
                if (props.getProperty("mongodb.password") != null) this.password = props.getProperty("mongodb.password");
                if (props.getProperty("mongodb.host") != null) this.host = props.getProperty("mongodb.host");
                if (props.getProperty("mongodb.database") != null) this.databaseName = props.getProperty("mongodb.database");
                if (props.getProperty("mongodb.authSource") != null) this.authSource = props.getProperty("mongodb.authSource");

                if (props.getProperty("db-username") != null) this.user = props.getProperty("db-username");
                if (props.getProperty("db-password") != null) this.password = props.getProperty("db-password");
                if (props.getProperty("db-host") != null) this.host = props.getProperty("db-host");
                if (props.getProperty("db-name") != null) this.databaseName = props.getProperty("db-name");
                if (props.getProperty("db-name") != null) this.authSource = props.getProperty("db-name");

                String portStr = props.getProperty("mongodb.port");
                if (portStr == null) portStr = props.getProperty("db-port");
                if (portStr != null) {
                    try {
                        this.port = Integer.parseInt(portStr);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException ex) {
            System.err.println("Info: Keine ppr.properties. Nutze Defaults.");
        }
    }

    /**
     * Erstellt den Client mit der modernen API (MongoClients.create).
     * @return Ein MongoClient Interface (aus com.mongodb.client)
     */
    public MongoClient createClient() {
        // Connection String bauen
        String connectionString = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s&connectTimeoutMS=10000&socketTimeoutMS=60000",
                user, password, host, port, databaseName, authSource
        );

        System.out.println("Erstelle MongoDB-Client (Modern API)...");
        System.out.println("Target: " + host + ":" + port + " | DB: " + databaseName);

        // HIER IST DER FIX: Nutze die Factory statt "new MongoClient(...)"
        return MongoClients.create(connectionString);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    // test main
    public static void main(String[] args) {
        System.out.println("--- Starte DB-Verbindungstest (Modern 5.x Mode) ---");
        MongoConnector connector = new MongoConnector();

        try (MongoClient client = connector.createClient()) {
            MongoDatabase db = client.getDatabase(connector.getDatabaseName());
            System.out.println("Client erstellt. Teste Zugriff auf DB: " + db.getName());

            boolean found = false;
            for (String name : db.listCollectionNames()) {
                System.out.println(" - Collection gefunden: " + name);
                found = true;
            }

            if (found) {
                System.out.println("ERFOLG: Verbindung funktioniert!");
            } else {
                System.out.println("ERFOLG: Verbindung steht (DB ist leer).");
            }

        } catch (Exception e) {
            System.err.println("FEHLER: Ein Fehler ist aufgetreten!");
            e.printStackTrace();
        }
        System.out.println("--- Test Ende ---");
    }
}