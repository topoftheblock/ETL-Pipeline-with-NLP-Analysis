package org.texttechnologylab.ppr.model.mongodb;

import org.bson.Document;
import org.texttechnologylab.ppr.model.interfaces.Kommentar;

import java.util.HashMap;
import java.util.Map;


/**
 * MongoDB-Repräsentation eines Kommentars (Zwischenruf/Beifall etc.).
 * Wird aus einem MongoDB Document gelesen und kann wieder als Map gespeichert werden.
 * @author Christian Block und Hamed Noori
 */

public class KommentarMongoDBImpl implements Kommentar {

    private String text;
    private int index;

    /**
     * Normaler Konstruktor, z.B. wenn wir Kommentare im Code erstellen.
     * @param text  Inhalt des Kommentars
     * @param index Position im Text (0 = vor dem ersten Absatz)
     */

    public KommentarMongoDBImpl(String text, int index) {
        this.text = text;
        this.index = index;
    }

    /**
     * Konstruktor zur Erstellung aus einem MongoDB Document.
     * @param doc Das Document aus der Datenbank.
     */

    public KommentarMongoDBImpl(Document doc) {
        this.text = doc.getString("text");
        Integer idx = doc.getInteger("index");
        this.index = (idx != null) ? idx : 0;
    }


    /** Gibt den Kommentartext zurück */

    @Override
    public String getText() {
        return text;
    }

    /** Setzt die Position des Kommentars */

    @Override
    public void setIndex(int index) {
        this.index = index;
    }


    /** Gibt die Position des Kommentars zurück */

    @Override
    public int getIndex() {
        return index;
    }


    /**
     * Wandelt das Objekt in eine Map um (praktisch zum Speichern als MongoDB Document).
     */

    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("text", this.text);
        map.put("index", this.index);
        return map;
    }

    /** JSON-Ausgabe ist hier identisch zur Node-Map */

    @Override
    public Map<String, Object> toJSON() {
        return toNode();
    }


    /** Kurze Debug-Ausgabe */

    @Override
    public String toString() {
        return "KommentarMongoDB: " + text;
    }
}