import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import models.LiftEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class LatencyTest {

    private static final String URL = "http://34.219.130.199:8080/Server_war/";
    private static final int TOTAL_REQUESTS = 10000;
    private SkiersApi skiersApi;

    @BeforeEach
    public void setUp() {
        ApiClient client = new ApiClient();
        client.setBasePath(URL);
        skiersApi = new SkiersApi(client);
    }

    @Test
    public void testRequestLatency() {
        DataGenerator generator = new DataGenerator(TOTAL_REQUESTS);
        Thread generatorThread = new Thread(generator);
        generatorThread.start();

        try {
            generatorThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Data generator finished");
        BlockingQueue<LiftEvent> eventQueue = generator.getGeneratedItems();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            try {
                LiftEvent event = eventQueue.take();
                postEvent(event);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgLatency = (double) totalTime / TOTAL_REQUESTS;

        System.out.println("Total time for " + TOTAL_REQUESTS + " requests: " + totalTime / 1000 + " seconds");
        System.out.println("Average latency per request: " + avgLatency + " ms");

        double acceptableLatencyMs = 500.0;
        assertTrue(avgLatency < acceptableLatencyMs, "Average latency exceeded acceptable threshold");
    }

    private void postEvent(LiftEvent event) {
        for (int i = 0; i < 5; i++) {
            try {
                ApiResponse<Void> response = skiersApi.writeNewLiftRideWithHttpInfo(
                        event.getRide(),
                        event.getResortID(),
                        String.valueOf(event.getSeasonID()),
                        String.valueOf(event.getDayID()),
                        event.getSkierID());

                if (response.getStatusCode() == 201) {
                    return;
                }
            } catch (ApiException e) {
                System.err.println("API error: " + e.getMessage());
            }
        }
    }
}
