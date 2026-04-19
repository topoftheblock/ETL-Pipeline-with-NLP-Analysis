package org.texttechnologylab.ppr.db;

import org.bson.Document;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.texttechnologylab.ppr.model.RednerImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
public class MongoDBConnectionTest {

    // Starts a throwaway MongoDB 6.0 container
    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    private MongoDBConnection dbConnection;

    @BeforeEach
    void setUp() {
        // Here we override the connection string to point to the Testcontainer
        // Note: Depending on how your MongoConnector.java is written, you might need
        // to add a constructor that accepts a custom URI for testing.
        String uri = mongoDBContainer.getReplicaSetUrl();

        // Assuming you can pass the URI directly for testing purposes:
        // dbConnection = new MongoDBConnection(uri);
    }

    @Test
    @DisplayName("Should correctly insert and count a Redner in the test database")
    void testInsertAndCountRedner() {
        // Note: Uncomment and adapt these lines based on your exact MongoDBConnection methods
        /*
        // 1. Arrange
        RednerImpl redner = new RednerImpl("12345");
        redner.setVorname("Test");
        redner.setNachname("Redner");

        // 2. Act
        dbConnection.createRedner(redner);
        long count = dbConnection.getRednerCount();

        // 3. Assert
        assertEquals(1, count, "Database should contain exactly 1 Redner");
        */
    }
}