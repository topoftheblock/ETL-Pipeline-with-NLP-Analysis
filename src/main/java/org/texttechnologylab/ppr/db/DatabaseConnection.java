package org.texttechnologylab.ppr.db;

import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set; // Neu importiert

/**
 * Interface für die Datenbankverbindung.
 * Erweitert nun CasStorage für die NLP-Persistierung (Aufgabe g).
 */
public interface DatabaseConnection extends CasStorage {

    void loescheDatenbank();
    void erstelleConstraints();

    // Methode zum Abrufen bereits vorhandener Sitzungen
    Set<String> getVorhandeneSitzungKeys();

    void ladeSitzungen(List<Sitzung> sitzungen);
    void ladeRedner(Collection<Redner> redner);
    void ladeRedenUndKommentare(List<Sitzung> sitzungen);
    void erstelleBeziehungen(List<Sitzung> sitzungen);

    void fuehreStatistikenAus();

    // CRUD Redner
    Redner createRedner(Redner redner);
    boolean updateRedner(String id, Map<String, Object> updateDaten);
    boolean deleteRedner(String id);
    List<Redner> getAbgeordnete(String suchbegriff, String sortierung);
    Redner getAbgeordneterDetails(String id);

    // Reden
    List<Rede> getRedenVonAbgeordnetem(String rednerId);
    Map<String, Object> getRedeDetails(String redeId);

    // Stats
    long getRednerCount();
    long getRedeCount();
    List<String> getAllFraktionen();
    List<Map<String, Object>> getTopRednerByLength(int limit);
    List<Map<String, Object>> getPartyByLength();
    List<Map<String, Object>> getTopRednerByComments(int limit);
    List<Map<String, Object>> getPartyByComments();
    Map<String, Object> getMaxTextSession();
    Map<String, Object> getMaxTimeSession();

    void close();

    //  NLP Methoden
    List<Rede> getAllReden();

    /**
     * Aktualisiert die NLP-Ergebnisse einer Rede.
     * JETZT NEU: Mit posStats und topicStats (List of Maps).
     */
    void updateRedeAnalysis(String id, double sentiment,
                            List<Map<String, Object>> topicStats,
                            Map<String, Integer> neMap,
                            Map<String, Integer> posStats);
}