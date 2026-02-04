package org.texttechnologylab.ppr.model.mongodb;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import org.bson.Document;
import org.hucompute.textimager.uima.type.Sentiment;
import org.hucompute.textimager.uima.type.category.CategoryCoveredTagged;
import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

public class RedeMongoDBImpl implements Rede {

    private String id;
    private List<String> textAbsatze;
    private List<Kommentar> kommentare;
    private int textLaenge;
    private Redner redner;
    private Sitzung sitzung;
    private int kommentarAnzahl;
    private Double sentiment;
    private List<String> topics;
    private Map<String, Integer> namedEntities;
    private Map<String, Integer> posStats;
    private List<Map<String, Object>> topicStats;
    private String casXmi;

    @SuppressWarnings("unchecked")
    public RedeMongoDBImpl(Document doc) {
        this.id = doc.getString("id");
        List<String> rawText = doc.getList("text", String.class);
        this.textAbsatze = rawText != null ? new ArrayList<>(rawText) : new ArrayList<>();
        this.textLaenge = doc.getInteger("textLaenge", 0);
        this.kommentare = new ArrayList<>();
        List<Document> kommentarDocs = doc.getList("kommentare", Document.class);
        if (kommentarDocs != null) {
            for (Document kDoc : kommentarDocs) {
                this.kommentare.add(new KommentarMongoDBImpl(kDoc));
            }
        }
        this.kommentarAnzahl = this.kommentare.size();
        this.sentiment = doc.getDouble("sentiment");
        this.topics = doc.getList("topics", String.class);
        if (this.topics == null) this.topics = new ArrayList<>();

        // Corrected catch blocks with identifiers
        try { this.namedEntities = (Map<String, Integer>) doc.get("namedEntities", Map.class); } catch (Exception e) { this.namedEntities = new HashMap<>(); }
        if (this.namedEntities == null) this.namedEntities = new HashMap<>();

        try { Map<?, ?> rawPos = doc.get("posStats", Map.class); this.posStats = rawPos != null ? (Map<String, Integer>) rawPos : new HashMap<>(); } catch (Exception e) { this.posStats = new HashMap<>(); }

        try {
            List<Document> rawTopics = doc.getList("topicStats", Document.class);
            this.topicStats = rawTopics != null ? new ArrayList<>(rawTopics) : new ArrayList<>();
        } catch (Exception e) {
            this.topicStats = new ArrayList<>();
        }
        this.casXmi = doc.getString("cas_xmi");
    }

    @Override public String getId() { return id; }
    @Override public String getVolltext() { return String.join("\n", textAbsatze); }
    @Override public void addAbsatz(String absatz) { this.textAbsatze.add(absatz); }
    @Override public void addKommentar(Kommentar kommentar) { this.kommentare.add(kommentar); }
    @Override public List<Kommentar> getKommentare() { return kommentare; }
    @Override public void setRedner(Redner redner) { this.redner = redner; }
    @Override public Redner getRedner() { return redner; }
    @Override public void setSitzung(Sitzung sitzung) { this.sitzung = sitzung; }
    @Override public Sitzung getSitzung() { return sitzung; }
    @Override public void setKommentarAnzahl(int anzahl) { this.kommentarAnzahl = anzahl; }
    @Override public int getKommentarAnzahl() { return !kommentare.isEmpty() ? kommentare.size() : kommentarAnzahl; }
    @Override public Double getSentiment() { return sentiment; }
    @Override public void setSentiment(Double sentiment) { this.sentiment = sentiment; }
    @Override public List<String> getTopics() { return topics; }
    @Override public void setTopics(List<String> topics) { this.topics = topics; }
    @Override public Map<String, Integer> getNamedEntities() { return namedEntities; }
    @Override public void setNamedEntities(Map<String, Integer> namedEntities) { this.namedEntities = namedEntities; }
    @Override public Map<String, Integer> getPosStats() { return posStats; }
    @Override public void setPosStats(Map<String, Integer> posStats) { this.posStats = posStats; }
    @Override public List<Map<String, Object>> getTopicStats() { return topicStats; }
    @Override public void setTopicStats(List<Map<String, Object>> topicStats) { this.topicStats = topicStats; }
    @Override public void setCasXmi(String xmi) { this.casXmi = xmi; }
    @Override public String getCasXmi() { return this.casXmi; }

    @Override
    public JCas toCAS() {
        try {
            JCas jcas = JCasFactory.createJCas();
            if (this.casXmi != null && !this.casXmi.isEmpty()) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(this.casXmi.getBytes(StandardCharsets.UTF_8))) {
                    CasIOUtils.load(bais, jcas.getCas());
                }
            } else {
                jcas.setDocumentText(this.getVolltext());
                jcas.setDocumentLanguage("de");
            }
            return jcas;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Erstellen des JCas für Rede " + this.id, e);
        }
    }

    @Override public List<String> getSentences() { return JCasUtil.select(toCAS(), Sentence.class).stream().map(Sentence::getCoveredText).collect(Collectors.toList()); }
    @Override public List<String> getTokens() { return JCasUtil.select(toCAS(), Token.class).stream().map(Token::getCoveredText).collect(Collectors.toList()); }
    @Override public List<String> getPosTags() { return JCasUtil.select(toCAS(), POS.class).stream().map(POS::getPosValue).collect(Collectors.toList()); }
    @Override public List<String> getNamedEntityValues() { return JCasUtil.select(toCAS(), NamedEntity.class).stream().map(NamedEntity::getValue).collect(Collectors.toList()); }

    @Override
    public List<Map<String, Object>> getSentencesWithSentiment() {
        List<Map<String, Object>> result = new ArrayList<>();
        JCas jcas = this.toCAS();
        String fullDocText = jcas.getDocumentText();
        int totalLength = (fullDocText != null) ? fullDocText.length() : 1;

        List<Sentence> sentences = new ArrayList<>(JCasUtil.select(jcas, Sentence.class));
        sentences.sort(Comparator.comparingInt(Sentence::getBegin));

        // Added topics collection for MongoDB class
        List<CategoryCoveredTagged> allTopics = new ArrayList<>(JCasUtil.select(jcas, CategoryCoveredTagged.class));

        if (sentences.isEmpty() && fullDocText != null) {
            BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.GERMAN);
            iterator.setText(fullDocText);
            int start = iterator.first();
            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("text", fullDocText.substring(start, end));
                entry.put("sentiment", 0.0);
                entry.put("begin", start); entry.put("end", end);
                entry.put("relStart", (double)start / totalLength);
                entry.put("relEnd", (double)end / totalLength);
                entry.put("posStats", new HashMap<>()); entry.put("neStats", new HashMap<>());
                entry.put("topics", new ArrayList<>());
                result.add(entry);
            }
        } else {
            for (Sentence sentence : sentences) {
                Map<String, Object> entry = new HashMap<>();
                int b = sentence.getBegin(); int e = sentence.getEnd();
                entry.put("text", sentence.getCoveredText());
                entry.put("begin", b); entry.put("end", e);
                entry.put("relStart", (double)b / totalLength);
                entry.put("relEnd", (double)e / totalLength);

                List<Sentiment> sentiments = JCasUtil.selectCovered(jcas, Sentiment.class, sentence);
                entry.put("sentiment", sentiments.isEmpty() ? 0.0 : sentiments.get(0).getSentiment());

                Map<String, Integer> localPos = new HashMap<>();
                for (POS pos : JCasUtil.selectCovered(jcas, POS.class, sentence)) {
                    if (pos.getPosValue() != null) localPos.merge(pos.getPosValue(), 1, Integer::sum);
                }
                entry.put("posStats", localPos);

                Map<String, Integer> localNe = new HashMap<>();
                for (NamedEntity ne : JCasUtil.selectCovered(jcas, NamedEntity.class, sentence)) {
                    if (ne.getValue() != null) localNe.merge(ne.getValue(), 1, Integer::sum);
                }
                entry.put("neStats", localNe);

                // Topics mapping using Overlap
                Map<String, Double> bestTopicMap = new HashMap<>();
                for (CategoryCoveredTagged cat : allTopics) {
                    if (cat.getBegin() < e && cat.getEnd() > b) {
                        if (cat.getValue() != null) bestTopicMap.merge(cat.getValue().trim(), cat.getScore(), Math::max);
                    }
                }
                List<Map<String, Object>> localTopics = new ArrayList<>();
                bestTopicMap.forEach((name, score) -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("name", name); t.put("score", score);
                    localTopics.add(t);
                });
                entry.put("topics", localTopics);

                result.add(entry);
            }
        }
        return result;
    }

    @Override public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("textLaenge", textLaenge > 0 ? textLaenge : getVolltext().length());
        if (sentiment != null) map.put("sentiment", sentiment);
        return map;
    }

    @Override public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>();
        json.put("id", id);
        json.put("textLaenge", textLaenge);
        json.put("kommentarAnzahl", getKommentarAnzahl());
        json.put("sentiment", sentiment);
        json.put("topics", topics);
        json.put("namedEntities", namedEntities);
        json.put("posStats", posStats);
        json.put("topicStats", topicStats);
        if (sitzung != null) {
            json.put("datum", sitzung.getDatum().toString());
            json.put("wp", sitzung.getWahlperiode());
            json.put("sitzungNr", sitzung.getSitzungNr());
        }
        return json;
    }
}