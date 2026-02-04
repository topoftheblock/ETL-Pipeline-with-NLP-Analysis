package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Redner;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ich Implementiere des Redner-Interfaces.Klasse repräsentiert einen Redner
 * im Plenarprotokoll-Kontext. Sie speichert persönliche Daten wie Name, Titel und Fraktions
 * sowie eine  ID.
 * @author Christian Block
 */
public class RednerImpl implements Redner {

    private String id;
    private String vorname;
    private String nachname;
    private String titel;
    private String fraktion;

    /**
     * neuer Redner mit  ID.
     * @param id  ID des Redners
     */
    public RednerImpl(String id) {
        this.id = id;
    }

    /**
     *  ID des Redners zurück.
     * @return  ID als String.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Vornamen des Redners zurück.
     * @return  Vorname
     */
    public String getVorname() { return vorname; }

    /**
     * n Nachnamen des Redners zurück.
     * @return Nachname
     */
    public String getNachname() { return nachname; }

    /**
     *  Titel des Redners zurück
     * @return Der Titel
     */
    public String getTitel() { return titel; }

    /**
     * Fraktions zugehörigkeit des Redners zurück.
     * @return Die Fraktion  oder {@code null}, wenn falls fraktionslos oder Gast. Ich weißt gar nicht, ob ein Gast überhaupt existiert :)
     */
    public String getFraktion() { return fraktion; }

    /**
     * Setzt den Vornamen
     * @param vorname  setzende Vorname.
     */
    public void setVorname(String vorname) { this.vorname = vorname; }

    /**
     * Setzt den Nachnamen
     * * @param nachname setzende Nachname.
     */
    public void setNachname(String nachname) { this.nachname = nachname; }

    /**
     * Setzt den Titels.
     * @param titel  setzende Titel.
     */
    public void setTitel(String titel) { this.titel = titel; }

    /**
     * Setzt die Fraktionsz ugehörigkeit .
     * @param fraktion setzende Fraktion.
     */
    public void setFraktion(String fraktion) { this.fraktion = fraktion; }

    /**
     *  vollständigen Namen des Redners, inklusive Titel als Rückgabe
     * @return Der formatierte vollständige Name.
     */
    @Override
    public String getVollerName() {
        return (titel != null && !titel.isEmpty() ? titel + " " : "") +
                (vorname != null ? vorname : "") + " " +
                (nachname != null ? nachname : "");
    }

    /**
     * Zwei speaker gelten als gleich, wenn ihre IDs identisch sind.
     * @param o  vergleichende Objekt.
     * @return {@code true}, wenn die Objekte gleich sind, sonst {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (o instanceof Redner) {
            return Objects.equals(id, ((Redner) o).getId());
        }
        return false;
    }

    /**
     * bestimmt den Hash-Code des Redners based auf seiner ID.
     * @return  Hash-Code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     *  eine String-Repräsentation des Redners zurück.
     * @return string;"Redner {ID}: {VollerName} ({Fraktion})".
     */
    @Override
    public String toString() {
        return "Redner " + id + ": " + getVollerName() + " (" + (fraktion != null ? fraktion : "N/A") + ")";
    }

    /**
     * create a  Map von Eigenschaften für die saving als Neo4j-Knoten.
     * @return  Map mit keys wie "id", "vorname", "nachname", "titel", "fraktion". und so weiter
     */
    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> props = new HashMap<>();
        props.put("id", this.id);
        props.put("vorname", this.vorname);
        props.put("nachname", this.nachname);
        props.put("titel", this.titel);
        props.put("fraktion", this.fraktion);
        return props;
    }

    /**
     * conert das Redner-Objekt in eine Map für die das JSON-Ausgabe.
     * @return  Map, die die JSON-Struktur represented.
     */
    @Override
    public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>();
        json.put("id", this.id);
        json.put("vollerName", getVollerName());
        json.put("vorname", this.vorname);
        json.put("nachname", this.nachname);
        json.put("titel", this.titel);
        json.put("fraktion", this.fraktion);
        return json;
    }
}