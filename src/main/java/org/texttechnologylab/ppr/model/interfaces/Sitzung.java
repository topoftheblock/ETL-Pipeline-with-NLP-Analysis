package org.texttechnologylab.ppr.model.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface für eine Plenarsitzung.
 */
public interface Sitzung {
    String getWahlperiode();
    String getSitzungNr();
    LocalDate getDatum();

    void setStartDateTime(LocalDateTime start);
    LocalDateTime getStartDateTime();//see discssuin on the implementation; maybe usage in the future

    void setEndDateTime(LocalDateTime end);
    LocalDateTime getEndDateTime();//see discssuin on the implementation; maybe usage in the future

    void addRede(Rede rede);
    List<Rede> getReden();

    Map<String, Object> toNode();
}