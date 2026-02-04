package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import java.util.HashMap;
import java.util.Map;

/**
 * Repräsentiert einen einzelnen Kommentar Zwischenruf oder Beifall. Zwischenruf funtkioniert aktuell nicht wie erwartet :(
 * (Hier löse ich die Anforderungen für Aufgabe 2a: Implementierung der Klassenstrukturen)
 * @author Christian Block
 */
public class KommentarImpl implements Kommentar {

    private String inhalt;
    // damit  Speichere ich  die Position des Kommentars in der Rede, um später das Kommentar an der richtigen Stelle anzuzeigen
    private int index;

    /**
     * Erstellt einen neuen Kommentar.
     * @param inhalt Der Text des Kommentars.
     */
    public KommentarImpl(String inhalt) {
        this.inhalt = inhalt;
        this.index = 0;
    }

    @Override
    public String getText() {
        return this.inhalt;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    /**
     * Gibt eine String-Repräsentation zurück;nützlich für Debugging.
     */
    @Override
    public String toString() {
        return "Kommentar (" + index + "): \"" + (inhalt.length() > 50 ? inhalt.substring(0, 50) + "..." : inhalt) + "\"";
    }

    /**
     * Erstellt eine Map von Eigenschaften für Neo4j.
     */
    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", this.inhalt);
        map.put("index", this.index);
        return map;
    }

    @Override
    public Map<String, Object> toJSON() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", this.inhalt);
        map.put("index", this.index);
        return map;
    }
}