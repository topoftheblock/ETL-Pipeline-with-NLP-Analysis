package org.texttechnologylab.ppr.db;

/**
 * Interface für die Persistierung von UIMA CAS-Objekten.
 * (Aufgabe g)
 */
public interface CasStorage {

    /**
     * Speichert den XMI-Inhalt (serialisiertes CAS) für eine bestimmte ID.
     * @param id Die ID des Dokuments (z.B. Rede-ID).
     * @param xmiContent Der serialisierte XMI-String.
     */
    void saveCasXmi(String id, String xmiContent);

    /**
     * Lädt den XMI-Inhalt für eine bestimmte ID.
     * @param id Die ID des Dokuments.
     * @return Der XMI-String oder null, falls nicht vorhanden.
     */
    String getCasXmi(String id);
}