package org.texttechnologylab.ppr.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.texttechnologylab.ppr.model.*;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.texttechnologylab.ppr.model.mongodb.*;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.*;

/**
 * MongoDBConnection ist unsere Datenbank-Schicht.
 * Aktualisiert auf die moderne API.
 * @author Christian Block und Hamed Noori
 */
public class MongoDBConnection implements DatabaseConnection {

    // Das ist das Interface com.mongodb.client.MongoClient
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    private static final String COL_REDNER = "redner";
    private static final String COL_REDEN = "reden";
    private static final String COL_SITZUNGEN = "sitzungen";

    /**
     * Konstruktor:
     * Delegiert den Verbindungsaufbau an MongoConnector.
     */
    public MongoDBConnection() {
        MongoConnector connector = new MongoConnector();

        // createClient() gibt jetzt den passenden modernen Client zurück
        this.mongoClient = connector.createClient();
        this.database = mongoClient.getDatabase(connector.getDatabaseName());

        System.out.println("MongoDBConnection initialisiert mit Datenbank: " + database.getName());
    }

    private MongoCollection<Document> getRednerCol() { return database.getCollection(COL_REDNER); }
    private MongoCollection<Document> getRedenCol() { return database.getCollection(COL_REDEN); }
    private MongoCollection<Document> getSitzungenCol() { return database.getCollection(COL_SITZUNGEN); }


    /**
     * Löscht alle Collections.
     */
    @Override
    public void loescheDatenbank() {
        try {
            getRednerCol().drop();
            getRedenCol().drop();
            getSitzungenCol().drop();
            System.out.println("Datenbank wurde gelöscht.");
        } catch (Exception e) {
            System.err.println("Fehler beim Löschen der DB (evtl. leer): " + e.getMessage());
        }
    }

    @Override
    public void erstelleConstraints() {
        try {
            getRednerCol().createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
            getRedenCol().createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
            getSitzungenCol().createIndex(
                    Indexes.compoundIndex(Indexes.ascending("wahlperiode"), Indexes.ascending("sitzungNr")),
                    new IndexOptions().unique(true)
            );
        } catch (Exception e) {
            System.err.println("Hinweis: Indizes konnten nicht erstellt werden: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getVorhandeneSitzungKeys() {
        Set<String> keys = new HashSet<>();
        // Wir projizieren nur die beiden nötigen Felder, um Traffic zu sparen
        getSitzungenCol().find()
                .projection(fields(include("wahlperiode", "sitzungNr"), excludeId()))
                .forEach(doc -> {
                    String wp = doc.getString("wahlperiode");
                    String snr = doc.getString("sitzungNr");
                    if (wp != null && snr != null) {
                        keys.add(wp + "-" + snr);
                    }
                });
        return keys;
    }

    @Override
    public void ladeSitzungen(List<Sitzung> sitzungen) {
        if (sitzungen.isEmpty()) return;
        List<WriteModel<Document>> writes = new ArrayList<>();
        for (Sitzung s : sitzungen) {
            Document doc = new Document(s.toNode());
            Bson filter = combine(eq("wahlperiode", s.getWahlperiode()), eq("sitzungNr", s.getSitzungNr()));
            writes.add(new ReplaceOneModel<>(filter, doc, new ReplaceOptions().upsert(true)));
        }
        if (!writes.isEmpty()) {
            getSitzungenCol().bulkWrite(writes);
            System.out.println(writes.size() + " Sitzungen wurden verarbeitet (Upsert).");
        }
    }

    @Override
    public void ladeRedner(Collection<Redner> redner) {
        if (redner.isEmpty()) return;
        List<WriteModel<Document>> writes = new ArrayList<>();
        for (Redner r : redner) {
            Document doc = new Document(r.toNode());
            Bson filter = eq("id", r.getId());
            writes.add(new ReplaceOneModel<>(filter, doc, new ReplaceOptions().upsert(true)));
        }
        if (!writes.isEmpty()) {
            getRednerCol().bulkWrite(writes);
            System.out.println(writes.size() + " Redner wurden verarbeitet (Upsert).");
        }
    }

    @Override
    public void ladeRedenUndKommentare(List<Sitzung> sitzungen) {
        List<WriteModel<Document>> writes = new ArrayList<>();
        for (Sitzung s : sitzungen) {
            for (Rede r : s.getReden()) {
                Document redeDoc = new Document(r.toNode());
                List<Document> kommentareDocs = r.getKommentare().stream()
                        .map(k -> new Document(k.toNode()))
                        .collect(Collectors.toList());
                redeDoc.append("kommentare", kommentareDocs);
                Bson filter = eq("id", r.getId());
                writes.add(new ReplaceOneModel<>(filter, redeDoc, new ReplaceOptions().upsert(true)));
            }
        }
        if (!writes.isEmpty()) {
            getRedenCol().bulkWrite(writes);
            System.out.println(writes.size() + " Reden wurden verarbeitet (Upsert).");
        }
    }

    @Override
    public void erstelleBeziehungen(List<Sitzung> sitzungen) {
        int count = 0;
        List<WriteModel<Document>> updates = new ArrayList<>();

        for (Sitzung s : sitzungen) {
            for (Rede r : s.getReden()) {
                if (r.getRedner() != null) {
                    Bson filter = eq("id", r.getId());
                    Bson update = combine(
                            set("rednerId", r.getRedner().getId()),
                            set("sitzungWp", s.getWahlperiode()),
                            set("sitzungNr", s.getSitzungNr()),
                            set("datum", s.getDatum().toString())
                    );
                    updates.add(new UpdateOneModel<>(filter, update));
                    count++;
                }
            }
        }
        if (!updates.isEmpty()) {
            getRedenCol().bulkWrite(updates);
            System.out.println(count + " Beziehungen (Rede -> Redner/Sitzung) aktualisiert.");
        }
    }

    @Override
    public void fuehreStatistikenAus() {
        System.out.println("Anzahl Redner in DB: " + getRednerCount());
        System.out.println("Anzahl Reden in DB: " + getRedeCount());
    }

    @Override
    public Redner createRedner(Redner redner) {
        Document doc = new Document(redner.toNode());
        getRednerCol().insertOne(doc);
        return redner;
    }

    @Override
    public boolean updateRedner(String id, Map<String, Object> updateDaten) {
        Bson filter = eq("id", id);
        Document updateDoc = new Document();
        updateDaten.forEach(updateDoc::append);
        if (updateDoc.isEmpty()) return true;

        var result = getRednerCol().updateOne(filter, new Document("$set", updateDoc));
        return result.getMatchedCount() > 0;
    }

    @Override
    public boolean deleteRedner(String id) {
        var result = getRednerCol().deleteOne(eq("id", id));
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<Redner> getAbgeordnete(String suchbegriff, String sortierung) {
        List<Bson> pipeline = new ArrayList<>();

        if (suchbegriff != null && !suchbegriff.isEmpty()) {
            Pattern pattern = Pattern.compile(suchbegriff, Pattern.CASE_INSENSITIVE);
            pipeline.add(match(or(regex("vorname", pattern), regex("nachname", pattern))));
        }

        if (sortierung != null && sortierung.contains("reden")) {
            pipeline.add(lookup(COL_REDEN, "id", "rednerId", "reden"));
            pipeline.add(addFields(new Field("redeAnzahl", new Document("$size", "$reden"))));
            pipeline.add(project(new Document("reden", 0)));
        }

        Bson sortSpec = Sorts.ascending("nachname", "vorname");
        if (sortierung != null) {
            switch (sortierung.toLowerCase()) {
                case "fraktion": sortSpec = Sorts.ascending("fraktion", "nachname"); break;
                case "fraktion_desc": sortSpec = Sorts.descending("fraktion", "nachname"); break;
                case "name_desc": sortSpec = Sorts.descending("nachname", "vorname"); break;
                case "reden": sortSpec = Sorts.descending("redeAnzahl"); break;
                case "reden_asc": sortSpec = Sorts.ascending("redeAnzahl"); break;
            }
        }
        pipeline.add(sort(sortSpec));

        List<Redner> result = new ArrayList<>();
        for (Document doc : getRednerCol().aggregate(pipeline)) {
            result.add(documentToRedner(doc));
        }
        return result;
    }

    @Override
    public Redner getAbgeordneterDetails(String id) {
        Document doc = getRednerCol().find(eq("id", id)).first();
        return documentToRedner(doc);
    }

    private Redner documentToRedner(Document doc) {
        if (doc == null) return null;
        String id = doc.getString("id");
        String fraktion = doc.getString("fraktion");
        RednerImpl r;
        if (fraktion != null && !fraktion.isEmpty()) {
            r = new AbgeordneterImpl(id);
        } else {
            r = new RednerImpl(id);
        }
        r.setVorname(doc.getString("vorname"));
        r.setNachname(doc.getString("nachname"));
        r.setTitel(doc.getString("titel"));
        r.setFraktion(fraktion);
        return r;
    }

    @Override
    public List<Rede> getRedenVonAbgeordnetem(String rednerId) {
        List<Rede> result = new ArrayList<>();
        FindIterable<Document> docs = getRedenCol().find(eq("rednerId", rednerId))
                .sort(Sorts.descending("datum"));

        for (Document doc : docs) {
            RedeImpl rede = new RedeImpl(doc.getString("id"));
            rede.setTextLaenge(doc.getInteger("textLaenge", 0));

            List<?> kommentare = doc.get("kommentare", List.class);
            rede.setKommentarAnzahl(kommentare != null ? kommentare.size() : 0);

            String dateStr = doc.getString("datum");
            if (dateStr != null) {
                rede.setSitzung(new SitzungImpl(doc.getString("sitzungWp"), doc.getString("sitzungNr"), LocalDate.parse(dateStr)));
            }
            result.add(rede);
        }
        return result;
    }

    @Override
    public Map<String, Object> getRedeDetails(String redeId) {
        Document redeDoc = getRedenCol().find(eq("id", redeId)).first();
        if (redeDoc == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("id", redeDoc.getString("id"));

        List<String> textLines = redeDoc.getList("text", String.class);
        result.put("text", textLines != null ? textLines : Collections.emptyList());

        List<Document> kommentare = redeDoc.getList("kommentare", Document.class);
        if (kommentare != null) {
            result.put("kommentare", kommentare.stream().map(d -> {
                Map<String, Object> k = new HashMap<>();
                k.put("text", d.getString("text"));
                k.put("index", d.getInteger("index"));
                return k;
            }).collect(Collectors.toList()));
        }

        String rednerId = redeDoc.getString("rednerId");
        if (rednerId != null) {
            Document rDoc = getRednerCol().find(eq("id", rednerId)).first();
            if (rDoc != null) {
                result.put("vorname", rDoc.getString("vorname"));
                result.put("nachname", rDoc.getString("nachname"));
                result.put("titel", rDoc.getString("titel"));
                result.put("fraktion", rDoc.getString("fraktion"));
                result.put("rednerId", rDoc.getString("id"));
            }
        }

        if (redeDoc.containsKey("sentiment")) {
            result.put("sentiment", redeDoc.getDouble("sentiment"));
        }
        if (redeDoc.containsKey("posStats")) {
            result.put("posStats", redeDoc.get("posStats", Map.class));
        }
        if (redeDoc.containsKey("namedEntities")) {
            result.put("namedEntities", redeDoc.get("namedEntities", Map.class));
        }
        if (redeDoc.containsKey("topicStats")) {
            result.put("topicStats", redeDoc.getList("topicStats", Document.class));
        }
        if (redeDoc.containsKey("cas_xmi")) {
            result.put("cas_xmi", redeDoc.getString("cas_xmi"));
        }

        return result;
    }

    @Override
    public long getRednerCount() {
        return getRednerCol().countDocuments();
    }

    @Override
    public long getRedeCount() {
        return getRedenCol().countDocuments();
    }

    @Override
    public List<String> getAllFraktionen() {
        return getRednerCol().distinct("fraktion", String.class)
                .filter(new Document("fraktion", new Document("$ne", null)))
                .into(new ArrayList<>())
                .stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopRednerByLength(int limit) {
        List<Bson> pipeline = Arrays.asList(
                group("$rednerId", Accumulators.avg("avgLen", "$textLaenge")),
                sort(Sorts.descending("avgLen")),
                limit(limit),
                lookup(COL_REDNER, "_id", "id", "redner"),
                unwind("$redner"),
                project(new Document("id", "$_id")
                        .append("vollerName", new Document("$concat", Arrays.asList("$redner.vorname", " ", "$redner.nachname")))
                        .append("fraktion", "$redner.fraktion")
                        .append("value", "$avgLen"))
        );
        List<Map<String, Object>> res = new ArrayList<>();
        for (Document doc : getRedenCol().aggregate(pipeline)) {
            res.add(doc);
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> getPartyByLength() {
        List<Bson> pipeline = Arrays.asList(
                lookup(COL_REDNER, "rednerId", "id", "redner"),
                unwind("$redner"),
                match(ne("redner.fraktion", null)),
                group("$redner.fraktion", Accumulators.avg("avgLen", "$textLaenge")),
                sort(Sorts.descending("avgLen")),
                project(new Document("name", "$_id").append("value", "$avgLen").append("_id", 0))
        );
        List<Map<String, Object>> res = new ArrayList<>();
        for (Document doc : getRedenCol().aggregate(pipeline)) {
            res.add(doc);
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> getTopRednerByComments(int limit) {
        List<Bson> pipeline = Arrays.asList(
                addFields(new Field("kommentarAnzahl", new Document("$size", new Document("$ifNull", Arrays.asList("$kommentare", Collections.emptyList()))))),
                group("$rednerId", Accumulators.avg("avgComments", "$kommentarAnzahl")),
                sort(Sorts.descending("avgComments")),
                limit(limit),
                lookup(COL_REDNER, "_id", "id", "redner"),
                unwind("$redner"),
                project(new Document("id", "$_id")
                        .append("vollerName", new Document("$concat", Arrays.asList("$redner.vorname", " ", "$redner.nachname")))
                        .append("fraktion", "$redner.fraktion")
                        .append("value", "$avgComments"))
        );
        List<Map<String, Object>> res = new ArrayList<>();
        for (Document doc : getRedenCol().aggregate(pipeline)) {
            res.add(doc);
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> getPartyByComments() {
        List<Bson> pipeline = Arrays.asList(
                addFields(new Field("kommentarAnzahl", new Document("$size", new Document("$ifNull", Arrays.asList("$kommentare", Collections.emptyList()))))),
                lookup(COL_REDNER, "rednerId", "id", "redner"),
                unwind("$redner"),
                match(ne("redner.fraktion", null)),
                group("$redner.fraktion", Accumulators.avg("avgComments", "$kommentarAnzahl")),
                sort(Sorts.descending("avgComments")),
                project(new Document("name", "$_id").append("value", "$avgComments").append("_id", 0))
        );
        List<Map<String, Object>> res = new ArrayList<>();
        for (Document doc : getRedenCol().aggregate(pipeline)) {
            res.add(doc);
        }
        return res;
    }

    @Override
    public Map<String, Object> getMaxTextSession() {
        List<Bson> pipeline = Arrays.asList(
                group(new Document("wp", "$sitzungWp").append("snr", "$sitzungNr"),
                        Accumulators.sum("totalLength", "$textLaenge"),
                        Accumulators.first("datum", "$datum")),
                sort(Sorts.descending("totalLength")),
                limit(1),
                project(new Document("wahlperiode", "$_id.wp")
                        .append("sitzungNr", "$_id.snr")
                        .append("datum", "$datum")
                        .append("value", "$totalLength"))
        );
        return getRedenCol().aggregate(pipeline).first();
    }

    @Override
    public Map<String, Object> getMaxTimeSession() {
        return getMaxTextSession();
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            System.out.println("Schließe MongoDB-Verbindung...");
            mongoClient.close();
        }
    }

    @Override
    public List<Rede> getAllReden() {
        List<Rede> result = new ArrayList<>();
        for (Document doc : getRedenCol().find()) {
            result.add(new RedeMongoDBImpl(doc));
        }
        return result;
    }

    @Override
    public void saveCasXmi(String id, String xmiContent) {
        getRedenCol().updateOne(eq("id", id), set("cas_xmi", xmiContent));
    }

    @Override
    public String getCasXmi(String id) {
        Document doc = getRedenCol().find(eq("id", id))
                .projection(include("cas_xmi"))
                .first();
        return doc != null ? doc.getString("cas_xmi") : null;
    }

    @Override
    public void updateRedeAnalysis(String id, double sentiment, List<Map<String, Object>> topicStats, Map<String, Integer> neMap, Map<String, Integer> posStats) {
        Bson filter = eq("id", id);

        List<Bson> updates = new ArrayList<>();
        updates.add(set("sentiment", sentiment));

        if (topicStats != null) {
            updates.add(set("topicStats", topicStats));
        }
        if (neMap != null) {
            updates.add(set("namedEntities", neMap));
        }
        if (posStats != null) {
            updates.add(set("posStats", posStats));
        }

        if (!updates.isEmpty()) {
            getRedenCol().updateOne(filter, combine(updates));
        }
    }
}