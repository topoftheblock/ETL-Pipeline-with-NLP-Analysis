package org.texttechnologylab.ppr.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.texttechnologylab.ppr.model.AbgeordneterImpl;
import org.texttechnologylab.ppr.model.RedeImpl;
import org.texttechnologylab.ppr.model.RednerImpl;
import org.texttechnologylab.ppr.model.SitzungImpl;
import org.texttechnologylab.ppr.model.interfaces.Kommentar;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j Implementierung.
 *  Enthält nun die Dummy-Methode getVorhandeneSitzungKeys(), damit der Compiler nicht meckert.
 */
public class Neo4jConnection implements DatabaseConnection {

    private final DatabaseManagementService managementService;
    private final GraphDatabaseService graphDb;
    private static final String DEFAULT_DB_NAME = "neo4j";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Neo4jConnection(String databaseDirectory) {
        System.out.println("Starte Embedded Neo4j-Datenbank in: " + databaseDirectory);
        Path dbPath = new File(databaseDirectory).toPath();
        this.managementService = new DatabaseManagementServiceBuilder(dbPath).build();
        this.graphDb = managementService.database(DEFAULT_DB_NAME);
        System.out.println("Neo4j-Datenbank erfolgreich gestartet.");
    }

    @Override
    public Set<String> getVorhandeneSitzungKeys() {
        return new HashSet<>(); // Leeres Set zurückgeben
    }

    private void executeWriteQuery(String query, Map<String, Object> parameters) {
        try (Transaction tx = graphDb.beginTx()) {
            tx.execute(query, parameters);
            tx.commit();
        } catch (Exception e) {
            System.err.println("Ups, Fehler bei Cypher-Query: " + query);
            e.printStackTrace();
        }
    }

    private void executeUnwindQuery(String query, List<Map<String, Object>> dataList, String listName) {
        try (Transaction tx = graphDb.beginTx()) {
            tx.execute(query, Map.of(listName, dataList));
            tx.commit();
        } catch (Exception e) {
            System.err.println("Fehler bei Cypher-UNWIND-Query: " + query);
            e.printStackTrace();
        }
    }

    private List<Map<String, Object>> executeReadList(String query, Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); Result result = tx.execute(query, params)) {
            return result.stream().collect(Collectors.toList());
        }
    }

    private Map<String, Object> executeReadOne(String query, Map<String, Object> params) {
        try (Transaction tx = graphDb.beginTx(); Result result = tx.execute(query, params)) {
            if (result.hasNext()) return result.next();
        }
        return null;
    }

    @Override
    public void loescheDatenbank() {
        executeWriteQuery("MATCH (n) DETACH DELETE n", Map.of());
    }

    @Override
    public void erstelleConstraints() {
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Sitzung) REQUIRE (s.sitzungNr, s.wahlperiode) IS UNIQUE", Map.of());
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Redner) REQUIRE r.id IS UNIQUE", Map.of());
        executeWriteQuery("CREATE CONSTRAINT IF NOT EXISTS FOR (r:Rede) REQUIRE r.id IS UNIQUE", Map.of());
    }

    @Override
    public void ladeSitzungen(List<Sitzung> sitzungen) {
        String query = "UNWIND $data AS props " +
                "MERGE (s:Sitzung { wahlperiode: props.wahlperiode, sitzungNr: props.sitzungNr }) " +
                "SET s.datum = date(props.datum), " +
                "    s.startDateTime = CASE WHEN props.startDateTime IS NOT NULL THEN datetime(props.startDateTime) ELSE null END, " +
                "    s.endDateTime = CASE WHEN props.endDateTime IS NOT NULL THEN datetime(props.endDateTime) ELSE null END";
        List<Map<String, Object>> data = sitzungen.stream().map(Sitzung::toNode).collect(Collectors.toList());
        executeUnwindQuery(query, data, "data");
    }

    @Override
    public void ladeRedner(Collection<Redner> redner) {
        String query = "UNWIND $data AS props " +
                "MERGE (r:Redner { id: props.id }) " +
                "SET r.vorname = props.vorname, " +
                "    r.nachname = props.nachname, " +
                "    r.titel = props.titel, " +
                "    r.fraktion = props.fraktion";
        List<Map<String, Object>> data = redner.stream().map(Redner::toNode).collect(Collectors.toList());
        executeUnwindQuery(query, data, "data");
        executeWriteQuery("MATCH (r:Redner) WHERE r.fraktion IS NOT NULL AND r.fraktion <> '' SET r:Abgeordneter", Map.of());
    }

    @Override
    public void ladeRedenUndKommentare(List<Sitzung> sitzungen) {
        String redeQuery = "UNWIND $data AS props MERGE (r:Rede { id: props.id }) SET r.text = props.text, r.textLaenge = props.textLaenge";
        List<Map<String, Object>> redenData = sitzungen.stream()
                .flatMap(s -> s.getReden().stream())
                .map(Rede::toNode)
                .collect(Collectors.toList());
        executeUnwindQuery(redeQuery, redenData, "data");

        String kommentarQuery = "UNWIND $data AS d MATCH (r:Rede { id: d.redeId }) UNWIND d.kommentare AS kProps CREATE (k:Kommentar) SET k = kProps MERGE (r)-[:BEINHALTET]->(k)";
        List<Map<String, Object>> kommentarData = sitzungen.stream()
                .flatMap(s -> s.getReden().stream())
                .map(rede -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("redeId", rede.getId());
                    map.put("kommentare", rede.getKommentare().stream()
                            .map(Kommentar::toNode)
                            .collect(Collectors.toList()));
                    return map;
                })
                .filter(map -> !((List<?>) map.get("kommentare")).isEmpty())
                .collect(Collectors.toList());
        executeUnwindQuery(kommentarQuery, kommentarData, "data");
    }

    @Override
    public void erstelleBeziehungen(List<Sitzung> sitzungen) {
        String query = "UNWIND $data AS d " +
                "MATCH (r:Rede { id: d.redeId }), (rn:Redner { id: d.rednerId }), (s:Sitzung { wahlperiode: d.sitzungWp, sitzungNr: d.sitzungNr }) " +
                "MERGE (rn)-[:HAT_GESPROCHEN]->(r) " +
                "MERGE (r)-[:GEHALTEN_IN]->(s)";

        List<Map<String, Object>> beziehungsDaten = sitzungen.stream()
                .flatMap(s -> s.getReden().stream()
                        .filter(r -> r.getRedner() != null)
                        .map(r -> Map.<String, Object>of(
                                "redeId", r.getId(),
                                "rednerId", r.getRedner().getId(),
                                "sitzungWp", s.getWahlperiode(),
                                "sitzungNr", s.getSitzungNr()
                        ))
                ).collect(Collectors.toList());

        executeUnwindQuery(query, beziehungsDaten, "data");
    }

    @Override
    public void fuehreStatistikenAus() {
    }

    @Override
    public Redner createRedner(Redner redner) {
        String query = "MERGE (r:Redner {id: $id}) " +
                "SET r.vorname = $vorname, " +
                "    r.nachname = $nachname, " +
                "    r.titel = $titel, " +
                "    r.fraktion = $fraktion";

        Map<String, Object> params = new HashMap<>();
        params.put("id", redner.getId());

        if (redner instanceof RednerImpl) {
            RednerImpl ri = (RednerImpl) redner;
            params.put("vorname", ri.getVorname());
            params.put("nachname", ri.getNachname());
            params.put("titel", ri.getTitel());
            params.put("fraktion", ri.getFraktion());
        }

        executeWriteQuery(query, params);

        if (redner instanceof RednerImpl && ((RednerImpl) redner).getFraktion() != null && !((RednerImpl) redner).getFraktion().isEmpty()) {
            executeWriteQuery("MATCH (r:Redner {id: $id}) SET r:Abgeordneter", Map.of("id", redner.getId()));
        }

        return redner;
    }

    @Override
    public boolean updateRedner(String id, Map<String, Object> updateDaten) {
        if (getAbgeordneterDetails(id) == null) return false;

        StringBuilder query = new StringBuilder("MATCH (r:Redner {id: $id}) SET ");
        Map<String, Object> params = new HashMap<>(updateDaten);
        params.put("id", id);

        List<String> sets = new ArrayList<>();
        if (updateDaten.containsKey("vorname")) sets.add("r.vorname = $vorname");
        if (updateDaten.containsKey("nachname")) sets.add("r.nachname = $nachname");
        if (updateDaten.containsKey("titel")) sets.add("r.titel = $titel");
        if (updateDaten.containsKey("fraktion")) sets.add("r.fraktion = $fraktion");

        if (sets.isEmpty()) return true;

        query.append(String.join(", ", sets));

        try (Transaction tx = graphDb.beginTx()) {
            tx.execute(query.toString(), params);
            tx.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteRedner(String id) {
        String query = "MATCH (r:Redner {id: $id}) DETACH DELETE r";
        if (getAbgeordneterDetails(id) == null) return false;
        executeWriteQuery(query, Map.of("id", id));
        return true;
    }

    private Redner mapToRedner(Map<String, Object> row) {
        if (row == null) return null;
        String id = (String) row.get("id");
        String fraktion = (String) row.getOrDefault("fraktion", "");

        RednerImpl r;
        if (fraktion != null && !fraktion.isEmpty()) {
            r = new AbgeordneterImpl(id);
        } else {
            r = new RednerImpl(id);
        }

        r.setVorname((String) row.getOrDefault("vorname", ""));
        r.setNachname((String) row.getOrDefault("nachname", ""));
        r.setTitel((String) row.getOrDefault("titel", ""));
        r.setFraktion(fraktion);
        return r;
    }

    @Override
    public List<Redner> getAbgeordnete(String search, String sort) {
        StringBuilder query = new StringBuilder("MATCH (a:Redner) ");
        Map<String, Object> params = new HashMap<>();

        String whereClause = "WHERE true ";
        if (search != null && !search.trim().isEmpty()) {
            whereClause += "AND (toLower(a.nachname) CONTAINS toLower($search) OR toLower(a.vorname) CONTAINS toLower($search)) ";
            params.put("search", search);
        }
        query.append(whereClause);

        boolean sortByReden = sort != null && sort.toLowerCase().contains("reden");
        if (sortByReden) {
            query.append("OPTIONAL MATCH (a)-[:HAT_GESPROCHEN]->(r:Rede) ");
        }

        query.append("RETURN a.id AS id, a.vorname AS vorname, a.nachname AS nachname, a.fraktion AS fraktion, a.titel AS titel ");

        if (sortByReden) {
            query.append(", count(r) AS redeAnzahl ");
        }

        String sortParam = sort != null ? sort.toLowerCase() : "name";
        switch (sortParam) {
            case "fraktion": query.append("ORDER BY a.fraktion ASC, a.nachname ASC"); break;
            case "fraktion_desc": query.append("ORDER BY a.fraktion DESC, a.nachname ASC"); break;
            case "name_desc": query.append("ORDER BY a.nachname DESC, a.vorname DESC"); break;
            case "reden": query.append("ORDER BY redeAnzahl DESC, a.nachname ASC"); break;
            case "reden_asc": query.append("ORDER BY redeAnzahl ASC, a.nachname ASC"); break;
            default: query.append("ORDER BY a.nachname ASC, a.vorname ASC");
        }

        return executeReadList(query.toString(), params).stream()
                .map(this::mapToRedner)
                .collect(Collectors.toList());
    }

    @Override
    public Redner getAbgeordneterDetails(String id) {
        String query = "MATCH (a:Redner {id: $id}) RETURN a.id AS id, a.vorname AS vorname, a.nachname AS nachname, a.fraktion AS fraktion, a.titel AS titel";
        return mapToRedner(executeReadOne(query, Map.of("id", id)));
    }

    @Override
    public List<Rede> getRedenVonAbgeordnetem(String rednerId) {
        String query = "MATCH (a:Redner {id: $id})-[:HAT_GESPROCHEN]->(r:Rede)-[:GEHALTEN_IN]->(s:Sitzung) " +
                "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                "WITH r, s, count(k) AS kommentarAnzahl " +
                "RETURN r.id AS id, r.textLaenge AS laenge, toString(s.datum) AS datum, s.sitzungNr AS sitzungNr, s.wahlperiode AS wp, kommentarAnzahl " +
                "ORDER BY s.datum DESC";

        List<Map<String, Object>> results = executeReadList(query, Map.of("id", rednerId));

        return results.stream().map(row -> {
                    try {
                        String redeId = (String) row.get("id");
                        RedeImpl rede = new RedeImpl(redeId);

                        Long commentsCount = (Long) row.get("kommentarAnzahl");
                        rede.setKommentarAnzahl(commentsCount != null ? commentsCount.intValue() : 0);

                        String dateStr = (String) row.get("datum");
                        String wp = (String) row.get("wp");
                        String snr = (String) row.get("sitzungNr");

                        if (dateStr != null && !dateStr.isEmpty() && !"null".equalsIgnoreCase(dateStr)) {
                            LocalDate datum = LocalDate.parse(dateStr);
                            SitzungImpl s = new SitzungImpl(wp, snr, datum);
                            rede.setSitzung(s);
                        }
                        return (Rede) rede;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRedeDetails(String redeId) {
        String query = "MATCH (r:Rede {id: $id})<-[:HAT_GESPROCHEN]-(a:Redner) " +
                "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                "RETURN r.text AS text, r.id AS id, a.vorname AS vorname, a.nachname AS nachname, " +
                "       a.titel AS titel, a.fraktion AS fraktion, a.id AS rednerId, " +
                "       r.sentiment AS sentiment, " +
                "       r.topicStats AS topicStats, " +
                "       r.namedEntities AS namedEntities, " +
                "       r.posStats AS posStats, " +
                "       collect({text: k.text, index: k.index}) AS kommentare";

        Map<String, Object> result = executeReadOne(query, Map.of("id", redeId));

        if (result != null) {
            try {
                if (result.get("namedEntities") instanceof String) {
                    result.put("namedEntities", objectMapper.readValue((String) result.get("namedEntities"), Map.class));
                }
                if (result.get("posStats") instanceof String) {
                    result.put("posStats", objectMapper.readValue((String) result.get("posStats"), Map.class));
                }
                if (result.get("topicStats") instanceof String) {
                    result.put("topicStats", objectMapper.readValue((String) result.get("topicStats"), List.class));
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Deserialisieren der NLP-Daten: " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    public long getRednerCount() {
        return (long) executeReadOne("MATCH (r:Redner) RETURN count(r) as c", Map.of()).get("c");
    }

    @Override
    public long getRedeCount() {
        return (long) executeReadOne("MATCH (r:Rede) RETURN count(r) as c", Map.of()).get("c");
    }

    @Override
    public List<String> getAllFraktionen() {
        return executeReadList("MATCH (r:Redner) WHERE r.fraktion IS NOT NULL AND r.fraktion <> '' RETURN DISTINCT r.fraktion as f ORDER BY f", Map.of())
                .stream().map(m -> (String) m.get("f")).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopRednerByLength(int limit) {
        String query = "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) " +
                "RETURN rn.id AS id, rn.vorname + ' ' + rn.nachname AS vollerName, rn.fraktion AS fraktion, avg(r.textLaenge) AS value " +
                "ORDER BY value DESC LIMIT $limit";
        return executeReadList(query, Map.of("limit", limit));
    }

    @Override
    public List<Map<String, Object>> getPartyByLength() {
        String query = "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) WHERE rn.fraktion IS NOT NULL " +
                "RETURN rn.fraktion AS name, avg(r.textLaenge) AS value ORDER BY value DESC";
        return executeReadList(query, Map.of());
    }

    @Override
    public List<Map<String, Object>> getTopRednerByComments(int limit) {
        String query = "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) " +
                "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                "WITH rn, r, count(k) as kCount " +
                "RETURN rn.id AS id, rn.vorname + ' ' + rn.nachname AS vollerName, rn.fraktion AS fraktion, avg(kCount) AS value " +
                "ORDER BY value DESC LIMIT $limit";
        return executeReadList(query, Map.of("limit", limit));
    }

    @Override
    public List<Map<String, Object>> getPartyByComments() {
        String query = "MATCH (rn:Redner)-[:HAT_GESPROCHEN]->(r:Rede) WHERE rn.fraktion IS NOT NULL " +
                "OPTIONAL MATCH (r)-[:BEINHALTET]->(k:Kommentar) " +
                "WITH rn, r, count(k) as kCount " +
                "RETURN rn.fraktion AS name, avg(kCount) AS value ORDER BY value DESC";
        return executeReadList(query, Map.of());
    }

    @Override
    public Map<String, Object> getMaxTextSession() {
        String query = "MATCH (s:Sitzung)<-[:GEHALTEN_IN]-(r:Rede) " +
                "RETURN s.wahlperiode AS wahlperiode, s.sitzungNr AS sitzungNr, toString(s.datum) AS datum, sum(r.textLaenge) AS value " +
                "ORDER BY value DESC LIMIT 1";
        return executeReadOne(query, Map.of());
    }

    @Override
    public Map<String, Object> getMaxTimeSession() {
        String query = "MATCH (s:Sitzung) WHERE s.startDateTime IS NOT NULL AND s.endDateTime IS NOT NULL " +
                "RETURN s.wahlperiode AS wahlperiode, s.sitzungNr AS sitzungNr, toString(s.datum) AS datum, " +
                "duration.between(s.startDateTime, s.endDateTime).minutes AS value " +
                "ORDER BY value DESC LIMIT 1";
        return executeReadOne(query, Map.of());
    }

    @Override
    public void close() {
        System.out.println("Fahre Neo4j-Datenbank herunter...");
        managementService.shutdown();
        System.out.println("Neo4j-Datenbank gestoppt. Tschüss!");
    }


    @Override
    public List<Rede> getAllReden() {
        String query = "MATCH (r:Rede) RETURN r.id AS id, r.text AS text";
        return executeReadList(query, Map.of()).stream().map(row -> {
            String id = (String) row.get("id");
            RedeImpl rede = new RedeImpl(id);
            Object textObj = row.get("text");
            if (textObj instanceof List) {
                for (Object line : (List<?>) textObj) rede.addAbsatz(line.toString());
            } else if (textObj instanceof String[]) {
                for (String line : (String[]) textObj) rede.addAbsatz(line);
            } else if (textObj instanceof String) {
                rede.addAbsatz((String) textObj);
            }
            return (Rede) rede;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveCasXmi(String id, String xmiContent) {
        String query = "MATCH (r:Rede {id: $id}) SET r.cas_xmi = $xmi";
        executeWriteQuery(query, Map.of("id", id, "xmi", xmiContent));
    }

    @Override
    public String getCasXmi(String id) {
        String query = "MATCH (r:Rede {id: $id}) RETURN r.cas_xmi AS xmi";
        Map<String, Object> result = executeReadOne(query, Map.of("id", id));
        if (result != null && result.containsKey("xmi")) {
            return (String) result.get("xmi");
        }
        return null;
    }

    @Override
    public void updateRedeAnalysis(String id, double sentiment, List<Map<String, Object>> topicStats, Map<String, Integer> neMap, Map<String, Integer> posStats) {
        String neJson = "{}";
        String posJson = "{}";
        String topicJson = "[]";

        try {
            if (neMap != null) neJson = objectMapper.writeValueAsString(neMap);
            if (posStats != null) posJson = objectMapper.writeValueAsString(posStats);
            if (topicStats != null) topicJson = objectMapper.writeValueAsString(topicStats);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        String query = "MATCH (r:Rede {id: $id}) " +
                "SET r.sentiment = $sentiment, " +
                "    r.topicStats = $topicJson, " +
                "    r.namedEntities = $neJson, " +
                "    r.posStats = $posJson";

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("sentiment", sentiment);
        params.put("topicJson", topicJson);
        params.put("neJson", neJson);
        params.put("posJson", posJson);

        executeWriteQuery(query, params);
    }
}