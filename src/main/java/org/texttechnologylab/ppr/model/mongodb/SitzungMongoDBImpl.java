package org.texttechnologylab.ppr.model.mongodb;

import org.bson.Document;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB-Repräsentation einer Sitzung.
 * Wird aus einem MongoDB Document gebaut und kann wieder als Map gespeichert werden.
 * Enthält Basisdaten (Wahlperiode, Sitzungsnummer, Datum) und optional Start/Ende-Zeit.
 * @author Christian Block und Hamed Noori
 */


public class SitzungMongoDBImpl implements Sitzung {

    private String wahlperiode;
    private String sitzungNr;
    private LocalDate datum;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private List<Rede> reden;


    /**
     * Konstruktor: erstellt eine Sitzung aus einem MongoDB Document.
     * Felder werden defensiv gelesen (falls etwas fehlt, bleibt es null).
     * @param doc Document aus der Datenbank (Collection: sitzungen)
     */

    public SitzungMongoDBImpl(Document doc) {
        this.wahlperiode = doc.getString("wahlperiode");
        this.sitzungNr = doc.getString("sitzungNr");

        String dateStr = doc.getString("datum");
        if (dateStr != null) {
            this.datum = LocalDate.parse(dateStr);
        }

        String start = doc.getString("startDateTime");
        if (start != null) this.startDateTime = LocalDateTime.parse(start);

        String end = doc.getString("endDateTime");
        if (end != null) this.endDateTime = LocalDateTime.parse(end);

        this.reden = new ArrayList<>();
    }

    @Override
    public String getWahlperiode() { return wahlperiode; }

    @Override
    public String getSitzungNr() { return sitzungNr; }

    @Override
    public LocalDate getDatum() { return datum; }

    @Override
    public void setStartDateTime(LocalDateTime start) { this.startDateTime = start; }

    @Override
    public LocalDateTime getStartDateTime() { return startDateTime; }

    @Override
    public void setEndDateTime(LocalDateTime end) { this.endDateTime = end; }

    @Override
    public LocalDateTime getEndDateTime() { return endDateTime; }

    @Override
    public void addRede(Rede rede) {
        this.reden.add(rede);
    }

    @Override
    public List<Rede> getReden() {
        return this.reden;
    }


    /**
     * Wandelt das Objekt in eine Map um (für MongoDB speichern).
     * Speichert nur Felder, die wirklich vorhanden sind.
     */

    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("wahlperiode", wahlperiode);
        map.put("sitzungNr", sitzungNr);
        if (datum != null) map.put("datum", datum.toString());
        if (startDateTime != null) map.put("startDateTime", startDateTime.toString());
        if (endDateTime != null) map.put("endDateTime", endDateTime.toString());
        return map;
    }
}