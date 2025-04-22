import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import models.LiftEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


class ApiPostClient implements Runnable {
    private BlockingQueue<LiftEvent> eventQueue;
    private CountDownLatch latch;
    private int numRequests;
    private SkiersApi skiersApi;

    private static final int RETRY_COUNT = 5;
    public static AtomicInteger successfulRequests = new AtomicInteger(0);
    public static AtomicInteger failedRequests = new AtomicInteger(0);

    public ApiPostClient(BlockingQueue<LiftEvent> eventQueue, CountDownLatch latch, int numRequests, String url) {
        this.eventQueue = eventQueue;
        this.latch = latch;
        this.numRequests = numRequests;

        ApiClient client = new ApiClient();
        client.setBasePath(url);
        this.skiersApi = new SkiersApi(client);
    }

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

    private void postEvent(LiftEvent event) {
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                ApiResponse<Void> response = skiersApi.writeNewLiftRideWithHttpInfo(
                        event.getRide(),
                        event.getResortID(),
                        String.valueOf(event.getSeasonID()),
                        String.valueOf(event.getDayID()),
                        event.getSkierID());

                if (response.getStatusCode() == 201) {
                    successfulRequests.incrementAndGet();
                    return;
                }
            } catch (ApiException e) {
                System.err.println("API error: " + e.getMessage());
            }
        }
        failedRequests.incrementAndGet();
    }
}
