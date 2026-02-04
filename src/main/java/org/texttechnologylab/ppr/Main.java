package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.config.PPRConfiguration;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.texttechnologylab.ppr.nlp.NLPPipeline;
import org.texttechnologylab.ppr.parser.XMLParser;
import org.texttechnologylab.ppr.rest.RESTHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Die Main-Klasse der Web-App.
 * @author Christian Block
 */
public class Main {

    /**
     * Startet die Anwendung.
     */
    public static void main(String[] args) {
        System.out.println("Suche nach XML-Protokollen und Videos in /resources...");
        List<String> xmlDateien;
        List<String> videoDateien;
        try {
            xmlDateien = findResources(".xml");
            videoDateien = findResources(".mp4");
        } catch (IOException | URISyntaxException e) {
            System.err.println("Fehler beim Lesen des Ordners: " + e.getMessage());
            return;
        }

        if (xmlDateien.isEmpty()) {
            System.err.println("Keine XML-Dateien gefunden!");
            return;
        }

        Collections.sort(xmlDateien);
        System.out.println("Gefundene XMLs: " + xmlDateien);
        System.out.println("Gefundene Videos (" + videoDateien.size() + "): " + videoDateien);

        try {
            PPRConfiguration config = new PPRConfiguration();
            AppFactory factory = AppFactory.getInstance();

            // 1. DB ZUERST VERBINDEN
            System.out.println("Verbinde zur Datenbank...");
            DatabaseConnection db = factory.createDatabaseConnection(config.getDatabasePath());

            // 2. Vorhandene Sitzungen abrufen
            Set<String> vorhandeneKeys = db.getVorhandeneSitzungKeys();

            System.out.println("--------------------------------------------------");
            System.out.println("Prüfe Datenbank auf bereits existierende Protokolle:");
            if (vorhandeneKeys.isEmpty()) {
                System.out.println(" -> Die Datenbank ist leer.");
            } else {
                vorhandeneKeys.stream()
                        .sorted() // Sortieren für bessere Übersicht
                        .forEach(key -> System.out.println(" -> Protokoll bereits vorhanden: " + key));
            }
            System.out.println("--------------------------------------------------");

            // 3. Parsing mit Prüfung auf Duplikate
            XMLParser parser = factory.getParser();
            System.out.println("Starte Parsing (Ignoriere " + vorhandeneKeys.size() + " bereits vorhandene Sitzungen)...");

            // Parser erhält die Liste der zu ignorierenden Sitzungen
            List<Sitzung> alleSitzungen = parser.parseFiles(xmlDateien, vorhandeneKeys);

            // Filterung nach Videos (nur für neu geparste Sitzungen)
            System.out.println("Filtere neue Reden ohne passende Videos...");
            List<Sitzung> gefilterteSitzungen = filterSitzungenNachVideos(alleSitzungen, videoDateien);

            if (!gefilterteSitzungen.isEmpty()) {
                System.out.println("Lade " + gefilterteSitzungen.size() + " NEUE Sitzungen in die Datenbank...");

                db.erstelleConstraints();
                db.ladeSitzungen(gefilterteSitzungen);

                Set<Redner> relevanteRedner = sammleRelevanteRedner(gefilterteSitzungen);
                db.ladeRedner(relevanteRedner);

                db.ladeRedenUndKommentare(gefilterteSitzungen);
                db.erstelleBeziehungen(gefilterteSitzungen);

                System.out.println("Datenbank-Upload der neuen Daten abgeschlossen.");
            } else {
                System.out.println("Keine neuen Sitzungen zu importieren.");
            }

            // 4. Statistik über ALLE Daten
            db.fuehreStatistikenAus();
            alleSitzungen = null;

            // 5. NLP Pipeline
            System.out.println(">>> Starte NLP-Analyse...");
            NLPPipeline nlp = new NLPPipeline(db, videoDateien);
            nlp.processAll();
            System.out.println(">>> NLP-Analyse abgeschlossen.");

            // 6. Webserver starten
            System.out.println("Initialisiere Webserver...");
            new RESTHandler(db, config, videoDateien);

            System.out.println("Anwendung läuft. http://localhost:" + config.getServerPort());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Sitzung> filterSitzungenNachVideos(List<Sitzung> sitzungen, List<String> availableVideos) {
        List<Sitzung> result = new ArrayList<>();
        for (Sitzung s : sitzungen) {
            List<Rede> redenMitVideo = new ArrayList<>();
            for (Rede r : s.getReden()) {
                String redeId = r.getId();
                boolean videoExists = availableVideos.stream().anyMatch(filename -> filename.startsWith(redeId));
                if (videoExists) {
                    redenMitVideo.add(r);
                }
            }
            if (!redenMitVideo.isEmpty()) {
                s.getReden().clear();
                for (Rede r : redenMitVideo) s.addRede(r);
                result.add(s);
            }
        }
        return result;
    }

    private static Set<Redner> sammleRelevanteRedner(List<Sitzung> sitzungen) {
        Set<Redner> redner = new HashSet<>();
        for (Sitzung s : sitzungen) {
            for (Rede r : s.getReden()) {
                if (r.getRedner() != null) redner.add(r.getRedner());
            }
        }
        return redner;
    }

    private static List<String> findResources(String suffix) throws IOException, URISyntaxException {
        ClassLoader classLoader = Main.class.getClassLoader();
        URL resourceUrl = classLoader.getResource("");
        if (resourceUrl == null) return Collections.emptyList();

        URI resourceUri = resourceUrl.toURI();
        Path resourcePath;

        if (resourceUri.getScheme().equals("jar")) {
            try {
                FileSystem fileSystem = FileSystems.getFileSystem(resourceUri);
                if (fileSystem == null) fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
                resourcePath = fileSystem.getPath("/");
            } catch (FileSystemAlreadyExistsException e) {
                resourcePath = FileSystems.getFileSystem(resourceUri).getPath("/");
            }
        } else {
            resourcePath = Paths.get(resourceUri);
        }

        try (Stream<Path> walk = Files.walk(resourcePath, 5)) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(suffix) && !name.equalsIgnoreCase("pom.xml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}