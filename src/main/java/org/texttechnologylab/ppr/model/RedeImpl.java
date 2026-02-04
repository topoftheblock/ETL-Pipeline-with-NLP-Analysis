package org.texttechnologylab.ppr.model;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
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

public class RedeImpl implements Rede {

    private String id;
    private List<String> absatze;
    private List<Kommentar> kommentare;
    private Redner redner;
    private Sitzung sitzung;
    private int textLaenge;
    private int kommentarAnzahl;
    private Double sentiment;
    private List<String> topics;
    private Map<String, Integer> namedEntities;
    private Map<String, Integer> posStats;
    private List<Map<String, Object>> topicStats;
    private String casXmi;

    public RedeImpl(String id) {
        this.id = id;
        this.absatze = new ArrayList<>();
        this.kommentare = new ArrayList<>();
        this.topics = new ArrayList<>();
        this.namedEntities = new HashMap<>();
        this.posStats = new HashMap<>();
        this.topicStats = new ArrayList<>();
    }

    @Override public String getId() { return id; }
    @Override public String getVolltext() { return String.join("\n", absatze); }
    public void setTextLaenge(int laenge) { this.textLaenge = laenge; }
    public int getTextLaenge() { return (this.textLaenge > 0) ? this.textLaenge : getVolltext().length(); }
    @Override public void addAbsatz(String absatz) { this.absatze.add(absatz); }
    @Override public void addKommentar(Kommentar kommentar) { this.kommentare.add(kommentar); }
    @Override public List<Kommentar> getKommentare() { return this.kommentare; }
    @Override public void setRedner(Redner redner) { this.redner = redner; }
    @Override public Redner getRedner() { return this.redner; }
    @Override public void setSitzung(Sitzung sitzung) { this.sitzung = sitzung; }
    @Override public Sitzung getSitzung() { return this.sitzung; }
    @Override public void setKommentarAnzahl(int anzahl) { this.kommentarAnzahl = anzahl; }
    @Override public int getKommentarAnzahl() { return (this.kommentare != null && !this.kommentare.isEmpty()) ? this.kommentare.size() : this.kommentarAnzahl; }
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
        int totalLength = (fullDocText != null && !fullDocText.isEmpty()) ? fullDocText.length() : 1;

        List<Sentence> sentences = new ArrayList<>(JCasUtil.select(jcas, Sentence.class));
        sentences.sort(Comparator.comparingInt(Sentence::getBegin));

        // Pre-collect all topics for overlapping check
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
                entry.put("relStart", (double) start / totalLength);
                entry.put("relEnd", (double) end / totalLength);
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
                entry.put("relStart", (double) b / totalLength);
                entry.put("relEnd", (double) e / totalLength);

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
                        String name = (cat.getValue() != null) ? cat.getValue().trim() : "";
                        if (!name.isEmpty()) bestTopicMap.merge(name, cat.getScore(), Math::max);
                    }
                }
                List<Map<String, Object>> localTopics = new ArrayList<>();
                bestTopicMap.forEach((name, score) -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("name", name); t.put("score", score);
                    localTopics.add(t);
                });
                localTopics.sort((t1, t2) -> Double.compare((Double) t2.get("score"), (Double) t1.get("score")));
                entry.put("topics", localTopics);

                result.add(entry);
            }
        }
        return result;
    }

    @Override public Map<String, Object> toJSON() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("textLaenge", getTextLaenge());
        map.put("kommentarAnzahl", getKommentarAnzahl());
        map.put("sentiment", sentiment);
        map.put("topics", topics);
        map.put("namedEntities", namedEntities);
        map.put("posStats", posStats);
        map.put("topicStats", topicStats);
        if (sitzung != null) {
            map.put("datum", sitzung.getDatum().toString());
            map.put("wp", sitzung.getWahlperiode());
            map.put("sitzungNr", sitzung.getSitzungNr());
        }
        return map;
    }

    @Override public Map<String, Object> toNode() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("textLaenge", getTextLaenge());
        if (sentiment != null) map.put("sentiment", sentiment);
        return map;
    }
}