package org.texttechnologylab.ppr.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.texttechnologylab.ppr.model.interfaces.Sitzung;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class XMLParserTest {

    @Test
    @DisplayName("Should extract Wahlperiode and SitzungNr correctly from XML")
    void testParseValidXML() {
        // Arrange: Load the dummy file from src/test/resources
        URL resource = getClass().getClassLoader().getResource("dummy-protocol.xml");
        assertNotNull(resource, "Dummy XML file not found!");
        File dummyXml = new File(resource.getFile());

        XMLParser parser = new XMLParser();
        Set<String> verarbeiteteSitzungen = new HashSet<>();

        // Act
        List<Sitzung> sessions = parser.parseFiles(List.of(dummyXml.getAbsolutePath()), verarbeiteteSitzungen);

        // Assert
        assertNotNull(sessions);
        assertEquals(1, sessions.size(), "Should parse exactly one session");

        Sitzung parsedSession = sessions.get(0);
        assertEquals("20", parsedSession.getWahlperiode());
        assertEquals("1", parsedSession.getSitzungNr());
        assertFalse(parsedSession.getReden().isEmpty(), "Session should contain speeches");
    }
}