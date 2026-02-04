package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.db.MongoDBConnection;
import org.texttechnologylab.ppr.db.Neo4jConnection; // Nicht mehr benötigt
import org.texttechnologylab.ppr.parser.XMLParser;

import java.io.IOException;


/**
 * Kleine Factory-Klasse, die zentrale Objekte erstellt.
 * Vorteil: Der Rest der App muss nicht wissen, welche konkrete DB-Klasse genutzt wird.
 */

public class AppFactory {

    private static AppFactory instance;

    private AppFactory() {}

    public static AppFactory getInstance() {
        if (instance == null) {
            instance = new AppFactory();
        }
        return instance;
    }

    public XMLParser getParser() {
        return new XMLParser();
    }

    /**
     * Erstellt die Datenbankverbindung.
     * HIER wurde die Änderung vorgenommen: Es wird nun die MongoDBConnection zurückgegeben.
     */
    public DatabaseConnection createDatabaseConnection(String connectionString) throws IOException {
        // Wir ignorieren hier den connectionString, da MongoDBConnection die Config
        // selbst aus der ppr.properties liest.
        return new MongoDBConnection();
    }
}