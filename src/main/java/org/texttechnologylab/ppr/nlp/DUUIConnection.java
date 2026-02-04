package org.texttechnologylab.ppr.nlp;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;

/**
 * Kapselt die Verbindung zur DUUI-Pipeline.
 * Idee: Wir bauen die Pipeline einmal auf und können dann viele Reden damit verarbeiten.
 * Nutzung der Remote-Services ist stabiler als lokale Container.
 * @author Christian Block und Hamed Noori
 */
public class DUUIConnection {

    private DUUIComposer composer;
    private boolean available = false;


    /**
     * Initialisiert die DUUI Pipeline:
     * - Lua Context (JSON)
     * - Drivers (UIMA + Remote)
     * - Komponenten: SpaCy, GerVader, ParlBert (alle remote)
     */

    public DUUIConnection() {
        try {
            // Setup
            DUUILuaContext ctx = new DUUILuaContext().withJsonLibrary();

            composer = new DUUIComposer()
                    .withLuaContext(ctx)
                    .withSkipVerification(true)
                    .withWorkers(4);

            DUUIUIMADriver uimaDriver = new DUUIUIMADriver();
            DUUIRemoteDriver remoteDriver = new DUUIRemoteDriver();

            composer.addDriver(uimaDriver, remoteDriver);

            // A) SpaCy (Remote)
            composer.add(new DUUIRemoteDriver.Component("http://spacy.service.component.duui.texttechnologylab.org")
                    .withScale(2)
                    .build());

            // B) GerVader (Remote)
            composer.add(new DUUIRemoteDriver.Component("http://gervader.service.component.duui.texttechnologylab.org")
                    .withParameter("selection", Sentence.class.getName())
                    .withScale(2)
                    .build());

            // C) ParlBert (Remote)
            composer.add(new DUUIRemoteDriver.Component("http://parlbert.service.component.duui.texttechnologylab.org")
                    .withParameter("selection", Sentence.class.getName())
                    .withScale(2)
                    .build());

            available = true;
            System.out.println("DUUI Remote-Pipeline erfolgreich initialisiert.");

        } catch (Throwable e) {
            System.err.println("WARNUNG: DUUI konnte nicht initialisiert werden.");
            e.printStackTrace();
            available = false;
        }
    }

    public void process(JCas jcas) throws Exception {
        if (available && composer != null) {
            composer.run(jcas);
        } else {
            throw new IllegalStateException("DUUI Pipeline ist nicht verfügbar.");
        }
    }

    public boolean isAvailable() {
        return available;
    }
}