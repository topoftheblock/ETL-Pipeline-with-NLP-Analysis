package org.texttechnologylab.ppr.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Dient der Verwaltung der Einstellung für die Anwendung für Übung drei. Es werden Schlässelpaare gespeichert
 * und dient als Initialiserung des Servers und lädt die Datei ppr.properties
 * Verwaltet die globalen Konfigurationseinstellungen der PPR-Anwendung.
 * sofern diese im Classpath vorhanden ist.
 * @author Christian Block
 */
public class PPRConfiguration extends Properties {

    /**
     * hier wird die   Konfigurationsdatei gesucht.
     */
    private static final String CONFIG_FILE = "ppr.properties";

    /**
     * Hier wird eine Konfigutarionsinstanz Erstellt.Wir benutzen zunächst die Standardwerte:
     * server.port: 7070
     * server.host: localhost
     * Anschließend  externen Eigenschaften mittels loadProperties()  laden.
     */
    public PPRConfiguration() {
        this.setProperty("server.port", "7070");
        this.setProperty("server.host", "localhost");

        loadProperties();
    }

    /**
     * Es wird hier in diesem Abschnitt Versucht die  ppr.properties-Datei aus dem Classpath in resources zu laden.
     * Wenn wird ddie Datei finden, werden die darin enthaltenen Werte in diese Properties-Instanz
     * geladen und überschreiben gegebenenfalls die Standardwerte.
     * Falls ein Fehler occured, wird eine Fehlermeldung auf der Konsole ausgegeben, und die Anwendung macht mit den Standardwerten weiter.
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.out.println("Keine '" + CONFIG_FILE + "' gefunden. Nutze Standardwerte (Port 7070).");
                return;
            }
            // hier werden die Properties aus der Datei und überschreibt ggf. auf Standartwerte
            this.load(input);
        } catch (IOException ex) {
            System.err.println("Fehler beim Laden der Konfiguration: " + ex.getMessage());
        }
    }

    /**
     * Hier rufe ich  den konfigurierten Port für den Webserver ab. Weiter lesen den Wert für den Schlüssel server.port.
     * Sollte der Wert nicht definiert sein oder keine gültige INteger darstellen, wird der Fehler abgefangen and der Standardport zurückgegeben.
     *  Rückgabe: Den Server-Port als int. Standardwert ist 7070, falls Fehler auftreten.
     */
    public int getServerPort() {
        try {
            return Integer.parseInt(this.getProperty("server.port"));
        } catch (NumberFormatException e) {
            System.err.println("Ungültiger Port in Konfiguration. Nutze 7070.");
            return 7070;
        }
    }

    /**
     * Ich Rufe den Dateipfad für die Datenbank ab.
     * Liest den Wert für den Key  database.path.
     * Rückgabe: Den Pfad zur Datenbank als String.
     * Wenn  nicht weiter konfiguriert, gib der Standardwert zurückgegeben.
     */
    public String getDatabasePath() {
        return this.getProperty("database.path", "target/neo4j-db");
    }
}