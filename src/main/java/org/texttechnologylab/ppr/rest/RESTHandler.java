package org.texttechnologylab.ppr.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.rendering.template.JavalinFreemarker;
import org.texttechnologylab.ppr.config.PPRConfiguration;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.RedeImpl;
import org.texttechnologylab.ppr.model.RednerImpl;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;

import java.io.InputStream;
import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sehr ad-hoc, wir sind froh ,dass jetzt alles funktioniert :)
 * RESTHandler - Web & API Layer.
 * Statt die NLP-Positionen unscharf zu projizieren, zerlegen wir den DB-Text
 * mit einem BreakIterator sauber in Sätze und matchen dann die NLP-Daten darauf.
 * Das garantiert korrekte Satzenden (Punkt, !) im Frontend.
 *
 */
public class RESTHandler {

    private final Javalin app;
    private final DatabaseConnection db;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> availableVideos;

    public RESTHandler(DatabaseConnection db, PPRConfiguration config, List<String> availableVideos) {
        this.db = db;
        this.availableVideos = availableVideos != null ? availableVideos : new ArrayList<>();

        this.app = Javalin.create(javalinConfig -> {
            javalinConfig.staticFiles.add("/public", Location.CLASSPATH);
            javalinConfig.fileRenderer(new JavalinFreemarker());

            javalinConfig.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDocumentationPath("/openapi");
                pluginConfig.withDefinitionConfiguration((version, definition) ->
                        definition.withInfo(info -> {
                            info.setTitle("PPR API");
                            info.setVersion("1.0");
                        })
                );
            }));
            javalinConfig.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.setUiPath("/swagger");
                swaggerConfig.setDocumentationPath("/openapi");
            }));
        });

        setupRoutes();
        this.app.start(config.getServerPort());
    }

    private void setupRoutes() {
        app.get("/api/redner", this::apiGetRedner);
        app.post("/api/redner", this::apiCreateRedner);
        app.get("/api/redner/{id}", this::apiGetRednerDetail);
        app.get("/api/redner/{id}/reden", this::apiGetRednerReden);
        app.get("/api/stats", this::apiGetStats);
        app.get("/api/video/{id}", this::streamVideo);

        app.get("/", this::viewIndex);
        app.get("/abgeordnete", this::viewAbgeordnetenListe);
        app.get("/abgeordneter/{id}", this::viewAbgeordneterDetail);
        app.get("/rede/{id}", this::viewRedeDetail);
        app.get("/stats", this::viewStats);
    }

    // API Handlers
    private void apiGetRedner(Context ctx) { ctx.json(db.getAbgeordnete(ctx.queryParam("q"), ctx.queryParam("sort"))); }
    private void apiCreateRedner(Context ctx) {
        try {
            Map body = ctx.bodyAsClass(Map.class);
            RednerImpl r = new RednerImpl((String) body.get("id"));
            r.setVorname((String) body.get("vorname"));
            r.setNachname((String) body.get("nachname"));
            r.setFraktion((String) body.get("fraktion"));
            db.createRedner(r);
            ctx.status(201).json(r.toJSON());
        } catch (Exception e) { ctx.status(400).result("Invalid Data"); }
    }
    private void apiGetRednerDetail(Context ctx) {
        Redner r = db.getAbgeordneterDetails(ctx.pathParam("id"));
        if (r != null) ctx.json(r.toJSON()); else ctx.status(404);
    }
    private void apiGetRednerReden(Context ctx) {
        List<Rede> reden = db.getRedenVonAbgeordnetem(ctx.pathParam("id"));
        if (reden != null) ctx.json(reden.stream().map(Rede::toJSON).collect(Collectors.toList())); else ctx.status(404);
    }
    private void apiGetStats(Context ctx) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("redner_count", db.getRednerCount());
        stats.put("rede_count", db.getRedeCount());
        ctx.json(stats);
    }
    private void streamVideo(Context ctx) {
        String id = ctx.pathParam("id");
        String filename = findVideoFilename(id);
        if (filename == null) { ctx.status(404).result("Video nicht gefunden."); return; }
        InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
        if (is == null) is = getClass().getClassLoader().getResourceAsStream("videos/" + filename);
        if (is != null) ctx.writeSeekableStream(is, "video/mp4"); else ctx.status(404).result("Videodatei konnte nicht geladen werden.");
    }

    // view handlers

    private void viewIndex(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titel", "Startseite");
        model.put("rednerCount", db.getRednerCount());
        model.put("count", db.getRedeCount());
        model.put("fraktionenListe", db.getAllFraktionen());
        model.put("abgeordnete", db.getAbgeordnete(ctx.queryParam("search"), "name"));
        model.put("search", ctx.queryParam("search"));
        model.put("fraktion", ctx.queryParam("fraktion"));
        ctx.render("templates/index.ftl", model);
    }

    private void viewAbgeordnetenListe(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titel", "Abgeordnete");
        model.put("rednerListe", db.getAbgeordnete(ctx.queryParam("q"), ctx.queryParam("sort")));
        model.put("currentSearch", ctx.queryParam("q"));
        model.put("currentSort", ctx.queryParam("sort"));
        ctx.render("templates/redner_liste.ftl", model);
    }

    private void viewAbgeordneterDetail(Context ctx) {
        String id = ctx.pathParam("id");
        Redner r = db.getAbgeordneterDetails(id);
        if (r == null) { ctx.status(404).result("Abgeordneter nicht gefunden"); return; }
        Map<String, Object> model = new HashMap<>();
        model.put("titel", r.getVollerName());
        model.put("redner", r);
        model.put("reden", db.getRedenVonAbgeordnetem(id));
        ctx.render("templates/redner_detail.ftl", model);
    }

    private void viewRedeDetail(Context ctx) {
        String id = ctx.pathParam("id");
        Map<String, Object> rawData = db.getRedeDetails(id);
        if (rawData == null) { ctx.status(404).result("Rede nicht gefunden"); return; }

        RedeImpl rede = hydrateRede(rawData);
        Redner redner = rede.getRedner();

        // 1. NLP-Rohdaten holen
        List<Map<String, Object>> nlpSentences = rede.getSentencesWithSentiment();

        // 2. DB-Text holen
        @SuppressWarnings("unchecked")
        List<String> paragraphs = (List<String>) rawData.getOrDefault("text", Collections.emptyList());
        String correctDbText = String.join("\n", paragraphs);
        int dbTextLen = correctDbText.length();

        // 3..Disclaimer: sätze segementieren, haben wir uns aus stack exchanage zusammengebaut
        // Statt zu projizieren, zerlegen wir den korrekten Text in echte Sätze.
        List<Map<String, Object>> realSentences = new ArrayList<>();

        if (dbTextLen > 0) {
            BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.GERMAN);
            iterator.setText(correctDbText);
            int start = iterator.first();

            for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
                String sentenceText = correctDbText.substring(start, end);

                // Leere Sätze überspringen
                if (sentenceText.trim().isEmpty()) continue;

                Map<String, Object> realS = new HashMap<>();
                realS.put("text", sentenceText); // Garantierter voller Satz inkl. Punkt
                realS.put("begin", start);
                realS.put("end", end);

                // Relativ-Position für Video-Sync berechnen
                realS.put("relStart", (double) start / dbTextLen);
                realS.put("relEnd", (double) end / dbTextLen);

                // 4.Disclaimer:aus stack exchange inspiriert worden!Bestes Sentiment aus NLP-Daten finden
                // Wir suchen den NLP-Satz, der am stärksten mit diesem echten Satz überlappt.
                double bestOverlap = 0.0;
                Map<String, Object> bestMatch = null;

                for (Map<String, Object> nlpS : nlpSentences) {
                    // NLP-Koordinaten auf DB-Text projizieren (grob)
                    double nlpRelStart = (double) nlpS.getOrDefault("relStart", 0.0);
                    double nlpRelEnd = (double) nlpS.getOrDefault("relEnd", 0.0);
                    int nlpStart = (int) (nlpRelStart * dbTextLen);
                    int nlpEnd = (int) (nlpRelEnd * dbTextLen);

                    // Intersection berechnen
                    int interStart = Math.max(start, nlpStart);
                    int interEnd = Math.min(end, nlpEnd);

                    if (interEnd > interStart) {
                        double overlapLen = interEnd - interStart;
                        if (overlapLen > bestOverlap) {
                            bestOverlap = overlapLen;
                            bestMatch = nlpS;
                        }
                    }
                }

                // Daten übernehmen
                if (bestMatch != null) {
                    realS.put("sentiment", bestMatch.getOrDefault("sentiment", 0.0));
                    realS.put("topics", bestMatch.getOrDefault("topics", new ArrayList<>()));
                    realS.put("posStats", bestMatch.getOrDefault("posStats", new HashMap<>()));
                    realS.put("neStats", bestMatch.getOrDefault("neStats", new HashMap<>()));
                } else {
                    realS.put("sentiment", 0.0);
                    realS.put("topics", new ArrayList<>());
                    realS.put("posStats", new HashMap<>());
                    realS.put("neStats", new HashMap<>());
                }

                realSentences.add(realS);
            }
        }

        // Global Index setzen
        for (int i = 0; i < realSentences.size(); i++) {
            realSentences.get(i).put("globalIndex", i);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawKommentare = (List<Map<String, Object>>) rawData.get("kommentare");
        // Wir nutzen jetzt realSentences statt nlpSentences
        List<Map<String, Object>> contentList = buildContentList(paragraphs, rawKommentare, realSentences);

        Map<String, Object> model = new HashMap<>();
        model.put("titel", "Rede " + id);
        model.put("rede", rede);
        model.put("redner", redner);
        model.put("contentList", contentList);
        model.put("videoAvailable", findVideoFilename(id) != null);

        try {
            model.put("posStatsJson", mapper.writeValueAsString(rawData.getOrDefault("posStats", Collections.emptyMap())));
            model.put("neStatsJson", mapper.writeValueAsString(rawData.getOrDefault("namedEntities", Collections.emptyMap())));
            model.put("sentencesJson", mapper.writeValueAsString(realSentences));
            model.put("topicStatsJson", mapper.writeValueAsString(cleanTopics(rawData.get("topicStats"))));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            model.put("error", "Fehler bei JSON-Generierung");
        }

        ctx.render("templates/rede_detail.ftl", model);
    }

    private void viewStats(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titel", "Statistiken");
        model.put("topLength", db.getTopRednerByLength(10));
        model.put("partyLength", db.getPartyByLength());
        model.put("topComm", db.getTopRednerByComments(10));
        model.put("partyComm", db.getPartyByComments());

        Map<String, Object> maxText = db.getMaxTextSession();
        if (maxText != null) {
            model.put("maxTextSession", maxText);
            model.put("maxTextLength", maxText.get("value"));
        }
        Map<String, Object> maxTime = db.getMaxTimeSession();
        if (maxTime != null) {
            model.put("maxTimeSession", maxTime);
            model.put("maxTimeDuration", maxTime.get("value"));
        }
        ctx.render("templates/stats.ftl", model);
    }

    //  helper methods

    private RedeImpl hydrateRede(Map<String, Object> data) {
        String id = (String) data.getOrDefault("id", "unknown");
        RedeImpl rede = new RedeImpl(id);

        RednerImpl redner = new RednerImpl((String) data.getOrDefault("rednerId", "unknown"));
        redner.setVorname((String) data.getOrDefault("vorname", "Unbekannt"));
        redner.setNachname((String) data.getOrDefault("nachname", ""));
        redner.setTitel((String) data.getOrDefault("titel", ""));
        redner.setFraktion((String) data.getOrDefault("fraktion", "fraktionslos"));
        rede.setRedner(redner);

        @SuppressWarnings("unchecked")
        List<String> paragraphs = (List<String>) data.getOrDefault("text", Collections.emptyList());
        for (String p : paragraphs) rede.addAbsatz(p);

        String xmi = (String) data.get("cas_xmi");
        if (xmi != null && !xmi.isBlank()) rede.setCasXmi(xmi);

        if (data.containsKey("sentiment")) rede.setSentiment((Double) data.get("sentiment"));

        return rede;
    }

    private List<Map<String, Object>> buildContentList(List<String> paragraphs,
                                                       List<Map<String, Object>> rawKommentare,
                                                       List<Map<String, Object>> realSentences) {
        List<Map<String, Object>> result = new ArrayList<>();

        Map<Integer, List<String>> commentsByIndex = new HashMap<>();
        if (rawKommentare != null) {
            for (Map<String, Object> k : rawKommentare) {
                int idx = (k.get("index") instanceof Number) ? ((Number) k.get("index")).intValue() : 0;
                String txt = (String) k.get("text");
                commentsByIndex.computeIfAbsent(idx, x -> new ArrayList<>()).add(txt);
            }
        }

        // Wir haben jetzt realSentences, die den gesamten Text lückenlos abdecken
        // Wir müssen sie nur noch den Absätzen zuordnen.
        // Da realSentences linear auf dem Fulltext basieren, können wir einfach Indices prüfen.

        int currentPos = 0; // Cursor im DB-Gesamttext

        if (commentsByIndex.containsKey(0)) {
            for (String c : commentsByIndex.get(0)) result.add(Map.of("type", "comment", "text", c));
        }

        for (int i = 0; i < paragraphs.size(); i++) {
            String pText = paragraphs.get(i);
            int pStart = currentPos;
            int pEnd = currentPos + pText.length();

            List<Map<String, Object>> pSentences = new ArrayList<>();
            for (Map<String, Object> s : realSentences) {
                int sBegin = (int) s.get("begin");
                int sEnd = (int) s.get("end");

                // Ein Satz gehört zum Absatz, wenn er größtenteils darin liegt
                int center = sBegin + (sEnd - sBegin) / 2;
                if (center >= pStart && center < pEnd) {
                    pSentences.add(s);
                }
            }

            // der Fallback: Sollte ein Absatz gar keine Sätze enthalten
            // Dann fügen wir ihn als Roh-Text hinzu, damit nichts fehlt.
            if (pSentences.isEmpty() && !pText.isBlank()) {
                Map<String, Object> chunk = new HashMap<>();
                chunk.put("text", pText);
                chunk.put("sentiment", 0.0);
                chunk.put("globalIndex", -1);
                pSentences.add(chunk);
            }

            Map<String, Object> pItem = new HashMap<>();
            pItem.put("type", "paragraph");
            pItem.put("index", i);
            pItem.put("sentences", pSentences);
            result.add(pItem);

            currentPos += pText.length() + 1;

            if (commentsByIndex.containsKey(i + 1)) {
                for (String c : commentsByIndex.get(i + 1)) result.add(Map.of("type", "comment", "text", c));
            }
        }
        return result;
    }

    private List<Map<String, Object>> cleanTopics(Object rawTopicsObj) {
        if (!(rawTopicsObj instanceof List)) return new ArrayList<>();
        List<?> rawList = (List<?>) rawTopicsObj;
        Map<String, Map<String, Object>> bestTopicMap = new HashMap<>();

        for (Object item : rawList) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> t = (Map<String, Object>) item;
                String name = (String) t.get("name");
                if (name == null) continue;
                String key = name.trim();
                double prob = 0.0;
                if (t.get("probability") instanceof Number) prob = ((Number) t.get("probability")).doubleValue();

                if (!bestTopicMap.containsKey(key) ||
                        prob > ((Number) bestTopicMap.get(key).get("probability")).doubleValue()) {
                    Map<String, Object> clean = new HashMap<>(t);
                    clean.put("name", key);
                    clean.put("probability", prob);
                    bestTopicMap.put(key, clean);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(bestTopicMap.values());
        result.sort((a, b) -> Double.compare((Double)b.get("probability"), (Double)a.get("probability")));
        return result;
    }

    private String findVideoFilename(String id) {
        if (availableVideos == null || availableVideos.isEmpty()) return null;
        return availableVideos.stream().filter(name -> name.startsWith(id)).findFirst().orElse(null);
    }
}