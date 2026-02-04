package org.texttechnologylab.ppr.parser;

import org.texttechnologylab.ppr.model.AbgeordneterImpl;
import org.texttechnologylab.ppr.model.KommentarImpl;
import org.texttechnologylab.ppr.model.RedeImpl;
import org.texttechnologylab.ppr.model.RednerImpl;
import org.texttechnologylab.ppr.model.SitzungImpl;
import org.texttechnologylab.ppr.model.interfaces.Rede;
import org.texttechnologylab.ppr.model.interfaces.Redner;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XMLParser {

    private final Map<String, Redner> rednerMap = new HashMap<>();
    private final Set<String> verarbeiteteSitzungen = new HashSet<>();
    private int duplikatZaehler = 0;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

    public Map<String, Redner> getRednerCache() {
        return rednerMap;
    }

    public List<Sitzung> parseFiles(List<String> filenames, Set<String> bereitsInDB) {
        verarbeiteteSitzungen.clear();
        if (bereitsInDB != null) verarbeiteteSitzungen.addAll(bereitsInDB);
        duplikatZaehler = 0;

        List<Sitzung> allSitzungen = new ArrayList<>();
        for (String filename : filenames) {
            System.out.println("Parse Datei: " + filename);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
                if (is != null) allSitzungen.addAll(parseSitzung(is));
                else System.err.println("ACHTUNG: Konnte Datei nicht finden: " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Parsing abgeschlossen.");
        System.out.println("  -> " + allSitzungen.size() + " Sitzungen neu geladen.");
        System.out.println("  -> " + duplikatZaehler + " Duplikate (oder bereits in DB) übersprungen.");
        System.out.println("  -> " + rednerMap.size() + " Redner im Cache.");
        return allSitzungen;
    }

    public List<Sitzung> parseFiles(List<String> filenames) {
        return parseFiles(filenames, new HashSet<>());
    }

    public List<Sitzung> parseSitzung(InputStream xmlInput) {
        List<Sitzung> sitzungen = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlInput);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            String wahlperiode = root.getAttribute("wahlperiode");
            String sitzungNr = root.getAttribute("sitzung-nr");
            String uniqueKey = wahlperiode + "-" + sitzungNr;

            if (verarbeiteteSitzungen.contains(uniqueKey)) {
                System.out.println("  -> INFO: Sitzung " + uniqueKey + " ist bereits in DB und wird übersprungen.");
                duplikatZaehler++;
                return sitzungen;
            }
            verarbeiteteSitzungen.add(uniqueKey);

            NodeList rednerListeNodes = root.getElementsByTagName("rednerliste");
            if (rednerListeNodes.getLength() > 0) parseRednerListe((Element) rednerListeNodes.item(0));

            String datumStr = root.getAttribute("sitzung-datum");
            String startStr = root.getAttribute("sitzung-start-uhrzeit");
            String endStr = root.getAttribute("sitzung-ende-uhrzeit");

            LocalDate datum = LocalDate.parse(datumStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            SitzungImpl sitzung = new SitzungImpl(wahlperiode, sitzungNr, datum);

            if (startStr != null && !startStr.isEmpty() && endStr != null && !endStr.isEmpty()) {
                try {
                    LocalTime startZeit = LocalTime.parse(startStr, timeFormatter);
                    LocalTime endeZeit = LocalTime.parse(endStr, timeFormatter);
                    LocalDateTime startDateTime = datum.atTime(startZeit);
                    LocalDateTime endDateTime = endeZeit.isBefore(startZeit) ? datum.plusDays(1).atTime(endeZeit) : datum.atTime(endeZeit);
                    sitzung.setStartDateTime(startDateTime);
                    sitzung.setEndDateTime(endDateTime);
                } catch (Exception e) {
                    System.err.println("Warnung: Uhrzeit konnte nicht geparst werden: " + e.getMessage());
                }
            }

            NodeList redeNodes = root.getElementsByTagName("rede");
            for (int i = 0; i < redeNodes.getLength(); i++) {
                Rede rede = parseRede((Element) redeNodes.item(i));
                if (rede != null) {
                    rede.setSitzung(sitzung);
                    sitzung.addRede(rede);
                }
            }
            sitzungen.add(sitzung);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sitzungen;
    }

    private void parseRednerListe(Element rednerListeElement) {
        NodeList rednerNodes = rednerListeElement.getElementsByTagName("redner");
        for (int i = 0; i < rednerNodes.getLength(); i++) extractAndCacheRedner((Element) rednerNodes.item(i));
    }

    private Rede parseRede(Element redeElement) {
        String id = redeElement.getAttribute("id");
        RedeImpl rede = new RedeImpl(id);
        NodeList childNodes = redeElement.getChildNodes();
        int absatzCounter = 0;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                String tagName = childElement.getTagName();

                if (tagName.equals("p")) {
                    String klasse = childElement.getAttribute("klasse");
                    boolean hasRednerTag = childElement.getElementsByTagName("redner").getLength() > 0;
                    if (klasse.equals("redner") || hasRednerTag) {
                        NodeList rNodes = childElement.getElementsByTagName("redner");
                        if (rNodes.getLength() > 0) rede.setRedner(extractAndCacheRedner((Element) rNodes.item(0)));
                    } else {
                        String text = childElement.getTextContent().trim();
                        if (!text.isEmpty()) {
                            rede.addAbsatz(text);
                            absatzCounter++;
                        }
                    }
                } else if (tagName.equals("kommentar")) {
                    String text = childElement.getTextContent().trim();
                    KommentarImpl kommentar = new KommentarImpl(text);
                    kommentar.setIndex(absatzCounter);
                    rede.addKommentar(kommentar);
                }
            }
        }
        return rede;
    }

    private Redner extractAndCacheRedner(Element rednerElement) {
        String id = rednerElement.getAttribute("id");
        if (id == null || id.isEmpty()) id = "unknown-" + System.currentTimeMillis() + Math.random();
        if (rednerMap.containsKey(id)) return rednerMap.get(id);

        String vorname = getTextContent(rednerElement, "vorname");
        String nachname = getTextContent(rednerElement, "nachname");
        String titel = getTextContent(rednerElement, "titel");
        String fraktion = getTextContent(rednerElement, "fraktion");

        if (nachname.isEmpty()) {
            String raw = getTextContent(rednerElement, "name");
            if (!raw.isEmpty()) {
                if (raw.contains("[")) {
                    int start = raw.indexOf("[");
                    int end = raw.indexOf("]");
                    if (start < end) {
                        fraktion = raw.substring(start + 1, end);
                        raw = raw.substring(0, start).trim();
                    }
                }
                int space = raw.lastIndexOf(' ');
                if (space > 0) {
                    vorname = raw.substring(0, space);
                    nachname = raw.substring(space + 1);
                } else {
                    nachname = raw;
                }
            } else {
                nachname = "Unbekannt";
            }
        }
        RednerImpl r;
        if (fraktion != null && !fraktion.isEmpty()) {
            fraktion = fraktion.replaceAll("\\s+", " ").trim();
            if (fraktion.equals("BÜNDNIS 90/ DIE GRÜNEN")) fraktion = "BÜNDNIS 90/DIE GRÜNEN";
            if (fraktion.equals("SPDCDU/CSU")) fraktion = "SPD";
            if (fraktion.equals("FRAKTIONSLOSE")) fraktion = "FRAKTIONSLOS";
            fraktion = fraktion.toUpperCase();
            r = new AbgeordneterImpl(id);
            r.setFraktion(fraktion);
        } else {
            r = new RednerImpl(id);
        }
        r.setVorname(vorname);
        r.setNachname(nachname);
        r.setTitel(titel);
        rednerMap.put(id, r);
        return r;
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) return nodes.item(0).getTextContent().trim();
        return "";
    }
}