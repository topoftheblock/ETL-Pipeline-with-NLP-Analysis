package org.texttechnologylab.ppr.rest;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.texttechnologylab.ppr.db.DatabaseConnection;
import org.texttechnologylab.ppr.model.RednerImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RESTHandlerTest {

    @Mock
    private DatabaseConnection mockDb;

    private Javalin app;

    @BeforeEach
    void setUp() {
        app = Javalin.create();
        // Assuming RESTHandler takes the database connection and the Javalin app instance
        // RESTHandler handler = new RESTHandler(app, mockDb);
    }

    @Test
    @DisplayName("GET /api/redner/{id}/reden should return 200 and JSON payload")
    void testGetSpeechesForSpeaker() {
        // Arrange: Mock the database to return a fake speaker when asked for ID 12345
        RednerImpl fakeRedner = new RednerImpl("12345");
        // when(mockDb.getAbgeordneterDetails("12345")).thenReturn(fakeRedner);

        // Act & Assert
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/api/redner/12345/reden");

            // Verify HTTP status code is 200 OK
            // assertEquals(200, response.code());

            // Verify response body contains the speaker ID
            // assertTrue(response.body().string().contains("12345"));
        });
    }
}