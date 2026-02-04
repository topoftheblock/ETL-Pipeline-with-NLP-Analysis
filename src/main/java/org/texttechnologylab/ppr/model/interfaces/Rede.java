package org.texttechnologylab.ppr.model.interfaces;

import org.apache.uima.jcas.JCas;
import java.util.List;
import java.util.Map; // Import für Map hinzufügen

public interface Rede {
    String getId();
    String getVolltext();

    // Bestehende Methoden
    void addAbsatz(String absatz);
    void addKommentar(Kommentar kommentar);
    List<Kommentar> getKommentare();

    void setRedner(Redner redner);
    Redner getRedner();

    void setSitzung(Sitzung sitzung);
    Sitzung getSitzung();

    void setKommentarAnzahl(int anzahl);
    int getKommentarAnzahl();

    // Statistische NLP-Daten
    Double getSentiment();
    void setSentiment(Double sentiment);
    List<String> getTopics();
    void setTopics(List<String> topics);
    Map<String, Integer> getNamedEntities();
    void setNamedEntities(Map<String, Integer> namedEntities);
    Map<String, Integer> getPosStats();
    void setPosStats(Map<String, Integer> posStats);
    List<Map<String, Object>> getTopicStats();
    void setTopicStats(List<Map<String, Object>> topicStats);

    //  für Aufgabe h und CAS-Handling
    JCas toCAS();

    void setCasXmi(String xmi);
    String getCasXmi();

    // Extraktionsmethoden für einzelne Merkmale
    List<String> getSentences();
    List<String> getTokens();
    List<String> getPosTags();
    List<String> getNamedEntityValues();

    List<Map<String, Object>> getSentencesWithSentiment();

    //  Visualisierung mit D3.js oder API nützliche NodeJSON-Methoden
    Map<String, Object> toNode();
    Map<String, Object> toJSON();
}