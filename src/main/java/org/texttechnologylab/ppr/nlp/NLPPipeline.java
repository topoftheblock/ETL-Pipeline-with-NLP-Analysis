package org.texttechnologylab.ppr.nlp;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.hucompute.textimager.uima.type.Sentiment;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.interfaces.Rede;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Führt die NLP-Analyse für alle Reden durch.
 * Angepasst auf das dynamische Video-Namensschema (ID + Zusatz + .mp4).
 */
public class NLPPipeline {

    private final DatabaseConnection db;
    private final DUUIConnection duui;
    private final WhisperService whisper;
    private final List<String> availableVideos;
    private JCas jcas;

    /**
     * Konstruktor erwartet nun die Liste der verfügbaren Videodateien.
     */
    public NLPPipeline(DatabaseConnection db, List<String> availableVideos) {
        this.db = db;
        this.availableVideos = availableVideos != null ? availableVideos : new ArrayList<>();
        this.duui = new DUUIConnection();
        this.whisper = new WhisperService();
    }

    public void processAll() {
        List<Rede> reden = db.getAllReden();
        System.out.println("Verarbeite " + reden.size() + " Reden...");

        int count = 0;
        for (Rede rede : reden) {
            try {
                processRede(rede);
                count++;
                if (count % 10 == 0) System.out.println(count + " Reden analysiert...");
            } catch (Exception e) {
                System.err.println("Fehler bei Rede " + rede.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("NLP abgeschlossen.");
    }

    private void processRede(Rede rede) throws Exception {
        boolean analysisRequired = true;
        String existingXmi = db.getCasXmi(rede.getId());

        if (existingXmi != null && !existingXmi.isEmpty()) {
            try {
                jcas = rede.toCAS();
                jcas.reset();
                try (ByteArrayInputStream bais = new ByteArrayInputStream(existingXmi.getBytes("UTF-8"))) {
                    CasIOUtils.load(bais, jcas.getCas());
                }
                if (JCasUtil.select(jcas, POS.class).size() > 0) {
                    analysisRequired = false;
                }
            } catch (Exception e) {
                jcas = null;
                analysisRequired = true;
            }
        }

        if (jcas == null) {
            jcas = rede.toCAS();
        }

        String text = jcas.getDocumentText();
        if (text == null || text.isEmpty()) return;

        boolean casChanged = false;

        if (analysisRequired && duui.isAvailable()) {
            duui.process(jcas);
            casChanged = true;
        }

        // Video-Pipeline mit dynamischer Dateinamensuche
        try {
            if (processVideoData(rede, jcas)) {
                casChanged = true;
            }
        } catch (Exception e) {
            System.err.println("Fehler in Video-Pipeline für Rede " + rede.getId() + ": " + e.getMessage());
        }

        if (casChanged) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CasIOUtils.save(jcas.getCas(), baos, SerialFormat.XMI);
            db.saveCasXmi(rede.getId(), baos.toString("UTF-8"));
        }

        updateStatistics(rede, jcas);
    }

    /**
     * Findet das Video anhand der Rede-ID (Präfix-Suche) und verarbeitet es.
     */
    private boolean processVideoData(Rede rede, JCas jcas) throws Exception {
        String redeId = rede.getId();

        // Suche nach einer Datei, die mit der Rede-ID beginnt
        String actualVideoFile = availableVideos.stream()
                .filter(name -> name.startsWith(redeId) && name.endsWith(".mp4"))
                .findFirst()
                .orElse(null);

        if (actualVideoFile == null) {
            return false;
        }

        URL videoUrl = this.getClass().getClassLoader().getResource(actualVideoFile);
        if (videoUrl == null) {
            videoUrl = this.getClass().getClassLoader().getResource("videos/" + actualVideoFile);
        }

        if (videoUrl != null) {
            File videoFile = new File(videoUrl.toURI());
            String videoPath = videoFile.getAbsolutePath();

            // Video View anlegen
            JCas videoView = jcas.createView("VideoRaw");
            videoView.setSofaDataURI(videoPath, "video/mp4");

            // Whisper Transkription
            String transcript = whisper.transcribe(videoPath);

            if (transcript != null && !transcript.isEmpty()) {
                JCas transcriptView = jcas.createView("TranscriptionView");
                transcriptView.setDocumentText(transcript);
                transcriptView.setDocumentLanguage("de");

                if (duui.isAvailable()) {
                    System.out.println("Analysiere Video-Transkript für Rede " + rede.getId());
                    duui.process(transcriptView);
                    return true;
                }
            }
        }
        return false;
    }

    private void updateStatistics(Rede rede, JCas jcas) {
        Map<String, Integer> posStats = new HashMap<>();
        for (POS pos : JCasUtil.select(jcas, POS.class)) {
            String value = pos.getPosValue();
            if (value != null) posStats.merge(value, 1, Integer::sum);
        }

        Map<String, Integer> neMap = new HashMap<>();
        for (NamedEntity ne : JCasUtil.select(jcas, NamedEntity.class)) {
            String value = ne.getValue();
            if (value != null) neMap.merge(value, 1, Integer::sum);
        }

        double sentiment = 0.0;
        var sentiments = JCasUtil.select(jcas, Sentiment.class);
        if (!sentiments.isEmpty()) {
            sentiment = sentiments.iterator().next().getSentiment();
        }

        Map<String, Map<String, Object>> uniqueTopics = new HashMap<>();
        for (CategoryCoveredTagged cat : JCasUtil.select(jcas, CategoryCoveredTagged.class)) {
            String topicName = cat.getValue();
            if (topicName == null) continue;
            topicName = topicName.trim();
            if (topicName.isEmpty()) continue;

            double score = cat.getScore();
            if (!uniqueTopics.containsKey(topicName) || score > (Double) uniqueTopics.get(topicName).get("probability")) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", topicName);
                m.put("probability", score);
                m.put("segments", List.of(Map.of("index", 0, "intensity", score)));
                uniqueTopics.put(topicName, m);
            }
        }

        List<Map<String, Object>> topicStats = uniqueTopics.values().stream()
                .sorted((m1, m2) -> Double.compare((Double)m2.get("probability"), (Double)m1.get("probability")))
                .limit(5)
                .collect(Collectors.toList());

        db.updateRedeAnalysis(rede.getId(), sentiment, topicStats, neMap, posStats);
    }
}