package org.texttechnologylab.ppr.model.mongodb;

import org.bson.Document;
import org.texttechnologylab.ppr.model.interfaces.Redner;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * MongoDB-Repräsentation eines Redners.
 * Dieses Objekt wird direkt aus einem MongoDB Document gebaut
 * und kann wieder als Map (für DB/JSON) ausgegeben werden.
 * @author Christian Block und Hamed Noori
 */

public class RednerMongoDBImpl implements Redner {

    private String id;
    private String vorname;
    private String nachname;
    private String titel;
    private String fraktion;

    public RednerMongoDBImpl(Document doc) {
        this.id = doc.getString("id");
        this.vorname = doc.getString("vorname");
        this.nachname = doc.getString("nachname");
        this.titel = doc.getString("titel");
        this.fraktion = doc.getString("fraktion");
    }

    @Override
    public String getId() {
        return id;
    }

    public String getVorname() { return vorname; }
    public String getNachname() { return nachname; }
    public String getTitel() { return titel; }
    public String getFraktion() { return fraktion; }

    // Setter für manuelle Anpassungen
    public void setVorname(String vorname) { this.vorname = vorname; }
    public void setNachname(String nachname) { this.nachname = nachname; }
    public void setTitel(String titel) { this.titel = titel; }
    public void setFraktion(String fraktion) { this.fraktion = fraktion; }


    /**
     * Vollständiger Name inkl. Titel (falls vorhanden).
     */

    @Override
    public String getVollerName() {
        return (titel != null && !titel.isEmpty() ? titel + " " : "") +
                (vorname != null ? vorname : "") + " " +
                (nachname != null ? nachname : "");
    }

    /**
     * Wandelt das Objekt in eine Map um (für MongoDB speichern).
     */

    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("vorname", vorname);
        map.put("nachname", nachname);
        map.put("titel", titel);
        map.put("fraktion", fraktion);
        return map;
    }


    /**
     * Zwei Redner sind gleich, wenn ihre IDs gleich sind.
     */

    @Override
    public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>(toNode());
        json.put("vollerName", getVollerName());
        return json;
    }


    /**
     * Zwei Redner sind gleich, wenn ihre IDs gleich sind.
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Redner)) return false;
        return Objects.equals(id, ((Redner) o).getId());
    }

    /** Hashcode basiert auf der ID */

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}