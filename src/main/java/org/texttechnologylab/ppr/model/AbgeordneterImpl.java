package org.texttechnologylab.ppr.model;

import org.texttechnologylab.ppr.model.interfaces.Abgeordneter;

/**
 * steht für einen einen Abgeordneten  spezialisierte Form eines Redners , denn erbt von RednerImpl
 * und wird vom Parser verwendet, fallls eine Fraktion im XML gefunden wird.
 * @author Christian Block
 *
 * FYI: Die Idee der Implementierung habe ich bereits in der Dokumentation von Übung 2 erläutert
 */
public class AbgeordneterImpl extends RednerImpl implements Abgeordneter {
    /**
     * Konstruiert einen neue Abgeordneter.
     * and Ruft den construktor der Basisklasse  RednerImpl auf.
     */
    public AbgeordneterImpl(String id) {
        super(id);
    }
}