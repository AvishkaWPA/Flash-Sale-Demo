package com.avishka.simulator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadTestSimulator {

    public static void main(String[] args) throws Exception {

        int concurrentUsers = 1000;
        int targetProductId = 8;

        String[] targetUrls = {
            "http://localhost:8080/api/v1/orders/buy",
            "http://localhost:8081/api/v1/orders/buy"
        };

        if (args.length > 0) {
            try {
                concurrentUsers = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        if (args.length > 1) {
            try {
                targetProductId = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        HttpClient client = HttpClient.newHttpClient();

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrentUsers);

        Queue<RequestResult> results =
                new ConcurrentLinkedQueue<>();

        // Start gate - all users wait here before sending requests
        CountDownLatch startGate = new CountDownLatch(1);

        System.out.println("========================================");
        System.out.println("       API LOAD TEST SIMULATOR");
        System.out.println("========================================");
        System.out.println("Target Endpoints: " + String.join(" | ", targetUrls));
        System.out.println("Product ID      : " + targetProductId);
        System.out.println("Concurrent Users: " + concurrentUsers);
        System.out.println("========================================");
        System.out.println();

        long testStartTime = System.nanoTime();

        for (int i = 0; i < concurrentUsers; i++) {

            final int userId = i + 1;
            String selectedUrl = targetUrls[i % targetUrls.length];

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(selectedUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"productId\":" + targetProductId + ",\"customerId\":"
                                    + userId
                                    + ",\"quantity\":1}"
                    ))
                    .build();

            executor.submit(() -> {

                try {
                    // Wait until main thread gives start signal
                    startGate.await();

                    long startTime = System.nanoTime();

                    HttpResponse<String> response =
                            client.send(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            );

                    long endTime = System.nanoTime();
                    long latencyMs = (endTime - startTime) / 1_000_000;
                    int statusCode = response.statusCode();
                    boolean success = statusCode >= 200 && statusCode < 300;

                    RequestResult result = new RequestResult(
                            userId,
                            statusCode,
                            latencyMs,
                            success
                    );

                    results.add(result);

                } catch (Exception e) {
                    results.add(new RequestResult(userId, -1, 0, false));
                }
            });
        }

        // Give all submitted users the start signal
        startGate.countDown();

        // Shutdown executor and wait for completion
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.MINUTES);
        long testEndTime = System.nanoTime();

        if (!finished) {
            System.out.println("WARNING: Some requests did not finish within 5 minutes.");
        }

        printResults(results, testStartTime, testEndTime, concurrentUsers);
    }

    private static void printResults(
            Queue<RequestResult> results,
            long testStartTime,
            long testEndTime,
            int totalRequests
    ) {

        List<RequestResult> resultList = new ArrayList<>(results);

        int successCount = 0;
        int failedCount = 0;
        long totalLatency = 0;
        long minLatency = Long.MAX_VALUE;
        long maxLatency = Long.MIN_VALUE;

        for (RequestResult result : resultList) {

            if (result.isSuccess()) {
                successCount++;
            } else {
                failedCount++;
            }

            long latency = result.getLatencyMs();
            totalLatency += latency;

            if (latency < minLatency) minLatency = latency;
            if (latency > maxLatency) maxLatency = latency;
        }

        double successRate = (successCount * 100.0) / totalRequests;
        double failureRate = (failedCount * 100.0) / totalRequests;
        double averageLatency = resultList.isEmpty() ? 0 : (double) totalLatency / resultList.size();
        double testDuration = (testEndTime - testStartTime) / 1_000_000_000.0;
        double throughput = testDuration > 0 ? totalRequests / testDuration : 0;

        System.out.println();
        System.out.println("========================================");
        System.out.println("           LOAD TEST RESULTS");
        System.out.println("========================================");

        System.out.println("Total Requests : " + totalRequests);
        System.out.println("Completed      : " + resultList.size());
        System.out.println("Successful     : " + successCount);
        System.out.println("Failed         : " + failedCount);

        System.out.printf("Success Rate   : %.2f%%%n", successRate);
        System.out.printf("Failure Rate   : %.2f%%%n", failureRate);

        System.out.println("----------------------------------------");

        if (!resultList.isEmpty()) {
            System.out.printf("Min Latency    : %d ms%n", minLatency);
            System.out.printf("Average Latency: %.2f ms%n", averageLatency);
            System.out.printf("Max Latency    : %d ms%n", maxLatency);
        }

        System.out.printf("Test Duration  : %.2f seconds%n", testDuration);
        System.out.printf("Throughput     : %.2f req/sec%n", throughput);

        System.out.println("========================================");
    }
}
