package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SitzungImpl implements Sitzung {

    private String wahlperiode;
    private String sitzungNr;
    private LocalDate datum;


    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private List<Rede> reden;

    public SitzungImpl(String wahlperiode, String sitzungNr, LocalDate datum) {
        this.wahlperiode = wahlperiode;
        this.sitzungNr = sitzungNr;
        this.datum = datum;
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
    public LocalDateTime getStartDateTime() { return startDateTime; } // noch aus Übung 2 ; vielelicht für die Zukunft :)

    @Override
    public void setEndDateTime(LocalDateTime end) { this.endDateTime = end; }

    @Override
    public LocalDateTime getEndDateTime() { return endDateTime; }// noch aus Übung 2 ; vielelicht für die Zukunft :)

    @Override
    public void addRede(Rede rede) { this.reden.add(rede); }

    @Override
    public List<Rede> getReden() { return this.reden; }

    @Override
    public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("wahlperiode", wahlperiode);
        map.put("sitzungNr", sitzungNr);
        map.put("datum", datum.toString());

        if (startDateTime != null) {
            map.put("startDateTime", startDateTime.toString());
        }
        if (endDateTime != null) {
            map.put("endDateTime", endDateTime.toString());
        }
        return map;
    }
}