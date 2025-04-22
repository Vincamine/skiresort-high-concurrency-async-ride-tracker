import models.LiftEvent;
import models.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class MultiThreadClient {

    // remote URL "http://[ec2-ip]:8080/Server_war/"
    // local URL "http://localhost:8080/"
    private static final String URL = "http://35.95.217.61:8080/Server_war/";
    private static final int TOTAL_REQUESTS = 200_000;
    private static final int INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int INITIAL_THREAD_COMPLETION_THRESHOLD = 1;

    public static void main(String[] args) throws InterruptedException, IOException {

        // Generate lift events
        DataGenerator generator = new DataGenerator(TOTAL_REQUESTS);
        Thread generatorThread = new Thread(generator);
        generatorThread.start();
        generatorThread.join();
        BlockingQueue<LiftEvent> eventQueue = generator.getGeneratedItems();
        System.out.println("Data generation completed, queue size: " + eventQueue.size());

        // List to store the records of API responses
        List<Record> recordList = new ArrayList<>();

        // Instance of ThroughputPlotWriter to collect and plot throughput data
        ThroughputPlotWriter throughputPlotWriter = new ThroughputPlotWriter();

        // Start
        long startTime = System.currentTimeMillis();

        // A scheduler to collect throughput data every second
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int totalRequests = ApiPostClient.successfulRequests.get() + ApiPostClient.failedRequests.get();
            double throughput = totalRequests / (elapsedTime / 1000.0);
            throughputPlotWriter.addThroughputData(elapsedTime / 1000.0, throughput);
        }, 1, 1, TimeUnit.SECONDS);

        // Create 32 threads and start
        ExecutorService initialExecutor = Executors.newFixedThreadPool(INITIAL_THREADS);
        CountDownLatch initialLatch = new CountDownLatch(INITIAL_THREAD_COMPLETION_THRESHOLD);

        // Submit API Post Client tasks to initial threads
        for (int i = 0; i < INITIAL_THREADS; i++) {
            initialExecutor.submit(new ApiPostClient(eventQueue, recordList, initialLatch, REQUESTS_PER_THREAD, URL));
        }

        initialLatch.await();
        System.out.println("One of initial threads has finished.");


        // Create remaining threads and continue
        int remainingRequests = TOTAL_REQUESTS - (INITIAL_THREADS * REQUESTS_PER_THREAD);
        int additionalThreads = (remainingRequests + REQUESTS_PER_THREAD - 1) / REQUESTS_PER_THREAD;
        ExecutorService remainingExecutor = Executors.newFixedThreadPool(additionalThreads);
        CountDownLatch remainingLatch = new CountDownLatch(additionalThreads);

        for (int i = 0; i < additionalThreads; i++) {
            remainingExecutor.submit(new ApiPostClient(eventQueue, recordList, remainingLatch, REQUESTS_PER_THREAD, URL));
        }

        remainingLatch.await();
        scheduler.shutdown();

        initialExecutor.shutdown();
        remainingExecutor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Print performance statistics
        printStats(duration);

        // Calculate and print more detailed statistics
        calculateAndPrintStatistics(recordList, duration);

        // Write the records to a CSV file
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.writeRecordsToCsv(recordList);

        // Plot throughput data
        throughputPlotWriter.plotThroughput();
    }

    /**
     * Prints basic statistics about the execution, including total requests, time taken, and throughput.
     *
     * @param duration The total time taken for the execution in milliseconds.
     */
    private static void printStats(long duration) {
        int totalRequestsSent = ApiPostClient.successfulRequests.get() + ApiPostClient.failedRequests.get();
        System.out.println("---------------------------------------------------");
        System.out.println("Total number of requests sent: " + totalRequestsSent +
                " (Success: " + ApiPostClient.successfulRequests.get() + ", Failures: " + ApiPostClient.failedRequests.get() + ")");
        System.out.println("Time taken: " + duration + " milliseconds");
        System.out.println("Throughput: " + (totalRequestsSent * 1000.0 / duration) + " requests/second");
    }

    /**
     * Calculates and prints detailed statistics for latency including mean, median, p99, min, and max.
     *
     * @param recordList The list of records containing latency information.
     * @param totalTimeMillis The total time taken for the test in milliseconds.
     */
    private static void calculateAndPrintStatistics(List<Record> recordList, long totalTimeMillis) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        List<Long> latencies = new ArrayList<>();

        for (Record record : recordList) {
            long latency = record.getLatency();
            latencies.add(latency);
            statistics.addValue(latency);
        }

        Collections.sort(latencies);

        double meanLatency = statistics.getMean();
        double medianLatency = statistics.getPercentile(50);
        long totalRequests = recordList.size();
        double throughput = totalRequests / (totalTimeMillis / 1000.0);
        double p99Latency = statistics.getPercentile(99);
        double minLatency = statistics.getMin();
        double maxLatency = statistics.getMax();

        System.out.println("---------------------------------------------------");
        System.out.println("Total number of requests: " + totalRequests);
        
        System.out.printf("Mean response time: %.2f ms\n", meanLatency);
        System.out.printf("Median response time: %.2f ms\n", medianLatency);
        System.out.printf("Throughput: %.2f requests/second\n", throughput);
        System.out.printf("99th percentile response time: %.2f ms\n", p99Latency);
        System.out.printf("Min response time: %.2f ms\n", minLatency);
        System.out.printf("Max response time: %.2f ms\n", maxLatency);
    }
}

