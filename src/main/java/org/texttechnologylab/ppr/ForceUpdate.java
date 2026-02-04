package org.texttechnologylab.ppr;

import org.texttechnologylab.ppr.config.PPRConfiguration;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.nlp.NLPPipeline;
import java.util.ArrayList;
import java.util.List;

public class ForceUpdate {
    public static void main(String[] args) throws Exception {
        System.out.println("Erzwinge NLP-Update...");
        PPRConfiguration config = new PPRConfiguration();
        DatabaseConnection db = AppFactory.getInstance().createDatabaseConnection(config.getDatabasePath());

        List<Rede> reden = db.getAllReden();
        if (!reden.isEmpty()) {
            Rede r = reden.get(0);
            System.out.println("Lösche alte Analyse für Rede: " + r.getId());

            // CAS löschen, um Neu-Analyse zu erzwingen
            db.saveCasXmi(r.getId(), null);

            // Pipeline starten
            NLPPipeline nlp = new NLPPipeline(db, new ArrayList<>());
            nlp.processAll();

            System.out.println("Fertig! Bitte prüfen Sie diese Rede im Browser: " + r.getId());
        }
        db.close();
    }
}