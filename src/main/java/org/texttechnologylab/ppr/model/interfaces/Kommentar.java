package org.texttechnologylab.ppr.model.interfaces;

import java.util.Map;

/**
 * Definiert das Interface für einen Kommentar innerhalb einer Rede.
 * (Hier löse ich die Anforderungen für Aufgabe 2c: Interfaces)
 * @author Christian Block
 */
public interface Kommentar {

    /**
     * Gibt den Textinhalt des Kommentars zurück.
     */
    String getText();

    /**
     * Setzt den Index des Kommentars relativ zu den Absätzen der Rede.
     * @param index Der Index des Absatzes, nach dem der Kommentar steht.
     */
    void setIndex(int index);

    /**
     * Gibt den Index  des Kommentars zurück.
     * @return Der Index.
     */
    int getIndex();

    /**
     * Erstellt eine Map von Eigenschaften für die Speicherung als Neo4j-Knoten.
     * (Hier löse ich die Anforderungen für Aufgabe 3c: toNode() Methode)
     */
    Map<String, Object> toNode();

    /**
     * Gibt die Daten als JSON-Map zurück.
     */
    Map<String, Object> toJSON();
}