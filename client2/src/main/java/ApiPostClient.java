import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import models.LiftEvent;
import models.Record;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ApiPostClient is a Runnable class responsible for posting lift ride events
 * to the Skiers API using multiple threads.
 */
class ApiPostClient implements Runnable {
    private BlockingQueue<LiftEvent> eventQueue;
    private List<Record> recordList;
    private CountDownLatch latch;
    private int numRequests;
    private SkiersApi skiersApi;

    private static final int RETRY_COUNT = 5;
    public static AtomicInteger successfulRequests = new AtomicInteger(0);
    public static AtomicInteger failedRequests = new AtomicInteger(0);

    /**
     * Constructor
     *
     * @param eventQueue BlockingQueue containing the LiftEvent objects to be posted
     * @param recordList List to record details of each request's latency and response status
     * @param latch CountDownLatch to signal when the thread has finished processing requests
     * @param numRequests Number of requests to be handled by this client
     * @param url Base URL for the Skiers API
     */
    public ApiPostClient(BlockingQueue<LiftEvent> eventQueue, List<Record> recordList, CountDownLatch latch, int numRequests, String url) {
        this.eventQueue = eventQueue;
        this.recordList = recordList;
        this.latch = latch;
        this.numRequests = numRequests;

        ApiClient client = new ApiClient();
        client.setBasePath(url);
        this.skiersApi = new SkiersApi(client);
    }

    /**
     * Main execution method for the Runnable. This method is called when the thread starts.
     * It processes the requests by taking events from the queue, making POST requests,
     * and tracking the results.
     */
    @Override
    public void run() {
        for (int i = 0; i < numRequests; i++) {
            try {
                LiftEvent event = eventQueue.take();
                postEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        latch.countDown();
    }

    /**
     * Sends a POST request for a given LiftEvent to the Skiers API and records the result.
     * Retries the request up to RETRY_COUNT times if it fails.
     *
     * @param event The LiftEvent to be posted
     */
    private void postEvent(LiftEvent event) {
        for (int i = 0; i < RETRY_COUNT; i++) {
            long startTime = System.currentTimeMillis();

            try {
                ApiResponse<Void> response = skiersApi.writeNewLiftRideWithHttpInfo(
                        event.getRide(),
                        event.getResortID(),
                        String.valueOf(event.getSeasonID()),
                        String.valueOf(event.getDayID()),
                        event.getSkierID());

                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                synchronized (recordList) {
                    recordList.add(new Record(startTime, "POST", latency, response.getStatusCode()));
                }

                if (response.getStatusCode() == 201) {
                    successfulRequests.incrementAndGet();
                    return;
                } 
            } catch (ApiException e) {
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                synchronized (recordList) {
                    recordList.add(new Record(startTime, "POST", latency, 500));
                }
                System.err.println("API error: " + e.getMessage());
            }
        }
        failedRequests.incrementAndGet();
    }
}
