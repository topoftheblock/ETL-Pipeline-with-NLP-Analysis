package org.texttechnologylab.ppr.nlp;

import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.factory.JCasFactory;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import java.io.File;

public class WhisperService {

    public String transcribe(String videoPath) {
        System.out.println("Starte Remote-Transkription für: " + videoPath);
        try {
            // 1. Temporäres CAS erstellen
            JCas tempCas = JCasFactory.createJCas();

            // Datei-URI sicher erstellen
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                System.err.println("Video-Datei nicht gefunden: " + videoPath);
                return "";
            }
            // Wir setzen das Video in die Standard-View (_InitialView)
            tempCas.setSofaDataURI(videoFile.toURI().toString(), "video/mp4");

            // 2. LUA-Kontext (wie in DUUIConnection)
            DUUILuaContext luaContext = new DUUILuaContext().withJsonLibrary();

            // 3. DUUI Composer mit Parametern für WhisperX
            DUUIComposer transcriber = new DUUIComposer()
                    .withLuaContext(luaContext)
                    .addDriver(new DUUIRemoteDriver())
                    .add(new DUUIRemoteDriver.Component("http://whisperx.service.component.duui.texttechnologylab.org")
                            // Sprache explizit auf Deutsch setzen
                            .withParameter("language", "de")
                            // Dem Treiber sagen, dass er die Standard-View als Audio-Quelle nutzen soll
                            .withParameter("selection", "_InitialView")
                            .build());

            // 4. Remote-Komponente ausführen
            transcriber.run(tempCas);

            // 5. Ergebnis auslesen
            String transcript = tempCas.getDocumentText();
            if (transcript != null && !transcript.isEmpty()) {
                return transcript;
            }

        } catch (Exception e) {
            System.err.println("Remote-Fehler bei WhisperX: " + e.getMessage());
        }

        // Fallback, damit die Pipeline bei Server-Fehlern nicht abbricht
        return "Transkription fehlgeschlagen (Server-Fehler 422). " +
                "Das Video unter " + videoPath + " konnte nicht verarbeitet werden.";
    }
}