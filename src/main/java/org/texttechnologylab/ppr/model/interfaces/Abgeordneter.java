package org.texttechnologylab.ppr.model.interfaces;

/**
 *  das Interface für einen Abgeordneten wird definiert
 * Ein Abgeordneter ist ein Redner, also erweitert Redner.Dieses Interface für Typ-Differenzierung.
 *  * @author Christian Block
 */
public interface Abgeordneter extends Redner {
    // This Interface erbt all Methoden von Redner
    // und dient als Interface zur Identifizierung von Abgeordneten.
    //Ist nicht opitmal :) kann man für die Zukunft auch löschne, aber ist schöner, dass jede Klasse in .Model auch ein Interface hat :) :)
}