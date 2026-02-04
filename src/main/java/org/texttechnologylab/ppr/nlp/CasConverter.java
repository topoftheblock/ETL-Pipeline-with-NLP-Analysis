package org.texttechnologylab.ppr.nlp;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.fit.factory.JCasFactory;
import org.texttechnologylab.ppr.db.DatabaseConnection; // Interface nutzen
import org.texttechnologylab.ppr.model.interfaces.Rede;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Hilfsklasse: erstellt für jede Rede ein leeres CAS (nur Text + Sprache)
 * und speichert es als XMI in der Datenbank.
 * Idee: So kann später die DUUI/NLP-Pipeline auf dem gleichen CAS weiterarbeiten,
 * und wir müssen nicht jedes Mal neu anfangen.
 * @author Christian Block und Hamed Noori
 */

public class CasConverter {

    /**
     * Liest alle Reden aus der DB, erstellt CAS-Objekte und speichert sie zurück.
     * Nutzt nun das generische Interface DatabaseConnection.
     * * @param db Die aktive Datenbankverbindung
     */
    public void convertRedenToCas(DatabaseConnection db) {
        System.out.println("Starte Konvertierung von Reden zu CAS...");

        // 1. Alle Reden laden
        List<Rede> alleReden = db.getAllReden();
        int count = 0;

        try {
            for (Rede rede : alleReden) {
                // 2. Leeres JCas erstellen
                JCas jcas = JCasFactory.createJCas();

                // 3. Text und Sprache setzen
                String text = rede.getVolltext();
                if (text == null) text = "";

                jcas.setDocumentText(text);
                jcas.setDocumentLanguage("de");
                // 4. Serialisierung zu XMI
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                CasIOUtils.save(jcas.getCas(), baos, SerialFormat.XMI);
                String xmiString = baos.toString("UTF-8");

                // 5. Speichern in DB
                db.saveCasXmi(rede.getId(), xmiString);

                count++;
                if (count % 100 == 0) {
                    System.out.println(count + " Reden verarbeitet...");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Fertig! " + count + " Reden wurden als CAS gespeichert.");
    }
}