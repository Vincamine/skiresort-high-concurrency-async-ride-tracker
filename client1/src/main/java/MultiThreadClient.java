import models.LiftEvent;
import java.util.concurrent.*;

public class MultiThreadClient {

    // remote URL "http://[ec2-ip]:8080/Server_war/"
    // local URL "http://localhost:8080/"
    private static final String URL = "http://35.95.217.61:8080/Server_war/";
    private static final int TOTAL_REQUESTS = 200_000;
    private static final int INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int INITIAL_THREAD_COMPLETION_THRESHOLD = 1;

    public static void main(String[] args) throws InterruptedException {
        // Start
        long startTime = System.currentTimeMillis();

        // Generate lift events
        DataGenerator generator = new DataGenerator(TOTAL_REQUESTS);
        Thread generatorThread = new Thread(generator);
        generatorThread.start();
        generatorThread.join();
        BlockingQueue<LiftEvent> eventQueue = generator.getGeneratedItems();
        System.out.println("Data generation completed, queue size: " + eventQueue.size());


        // Create 32 threads and start
        ExecutorService initialExecutor = Executors.newFixedThreadPool(INITIAL_THREADS);
        CountDownLatch initialLatch = new CountDownLatch(INITIAL_THREAD_COMPLETION_THRESHOLD);


        for (int i = 0; i < INITIAL_THREADS; i++) {
            initialExecutor.submit(new ApiPostClient(eventQueue, initialLatch, REQUESTS_PER_THREAD, URL));
        }

        initialLatch.await();
        long initialThreadFinishTime = System.currentTimeMillis();
        long duration1 = initialThreadFinishTime - startTime;
        System.out.println("---------------------------------------------------");
        System.out.println("Number of initial threads: " + INITIAL_THREADS);
        System.out.println("One of initial threads has finished.");
        System.out.println("Phase 1: " + duration1 + " milliseconds");

        // Create remaining threads and continue
        int remainingRequests = TOTAL_REQUESTS - (INITIAL_THREADS * REQUESTS_PER_THREAD);
        int additionalThreads = (remainingRequests + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD;
        ExecutorService remainingExecutor = Executors.newFixedThreadPool(additionalThreads);
        CountDownLatch remainingLatch = new CountDownLatch(additionalThreads);

        for (int i = 0; i < additionalThreads; i++) {
            remainingExecutor.submit(new ApiPostClient(eventQueue, remainingLatch, REQUESTS_PER_THREAD, URL));
        }

        remainingLatch.await();

        initialExecutor.shutdown();
        remainingExecutor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration2 = endTime - initialThreadFinishTime;
        long totalDuration = endTime - startTime;

        int totalRequestsSent = ApiPostClient.successfulRequests.get() + ApiPostClient.failedRequests.get();
        System.out.println("---------------------------------------------------");
        System.out.println("Total number of requests sent: " + totalRequestsSent +
                " (Success: " + ApiPostClient.successfulRequests.get() + ", Failures: " + ApiPostClient.failedRequests.get() + ")");
        System.out.println("Number of additional threads: " + additionalThreads);
        System.out.println("Phase 2: " + duration2 + " milliseconds");
        System.out.println("Total time taken: " + totalDuration + " milliseconds");
        System.out.println("Throughput: " + (totalRequestsSent * 1000.0 / totalDuration) + " requests/second");
    }
}

