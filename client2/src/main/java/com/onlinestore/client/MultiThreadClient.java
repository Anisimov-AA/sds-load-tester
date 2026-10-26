package com.onlinestore.client;

import java.io.BufferedWriter;                                 // NEW
import java.io.IOException;                                    // NEW
import java.net.URI;
import java.nio.file.Files;                                    // NEW
import java.nio.file.Path;                                     // NEW
import java.util.ArrayList;
import java.util.Arrays;                                       // NEW
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {

    private static final int TOTAL_REQUESTS = 200_000;
    private static final int QUEUE_CAPACITY = 1_000;

    public static void main(String[] args) throws InterruptedException, IOException {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 32;
        URI uri = URI.create(baseUrl + "/api/product");

        BlockingQueue<Product> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger successful = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        long start = System.currentTimeMillis();

        Thread producer = new Thread(new ProductProducer(queue, TOTAL_REQUESTS, threads));
        producer.start();

        List<PostWorker> workers = new ArrayList<>();          // NEW: keep the workers
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            PostWorker worker = new PostWorker(queue, uri, successful, failed);
            Thread thread = new Thread(worker);
            thread.start();
            workers.add(worker);
            threadList.add(thread);
        }

        producer.join();
        for (Thread thread : threadList) {
            thread.join();
        }

        long wallMs = System.currentTimeMillis() - start;
        double throughput = TOTAL_REQUESTS / (wallMs / 1000.0);

        // NEW 1: collect all notebooks into one list
        List<LatencyRecord> records = new ArrayList<>();
        for (PostWorker worker : workers) {
            records.addAll(worker.getRecords());
        }

        // NEW 2: write CSV after the test
        Path csv = Path.of("latencies.csv");
        try (BufferedWriter out = Files.newBufferedWriter(csv)) {
            out.write("start_ms,request_type,latency_ms,status_code");
            out.newLine();
            for (LatencyRecord r : records) {
                out.write(r.toCsv());
                out.newLine();
            }
        }

        // NEW 3: statistics on sorted latencies
        long[] latencies = new long[records.size()];
        for (int i = 0; i < latencies.length; i++) {
            latencies[i] = records.get(i).latencyMs();
        }
        Arrays.sort(latencies);
        long sum = 0;
        for (long l : latencies) {
            sum += l;
        }
        int n = latencies.length;
        double mean = (double) sum / n;
        double median = n % 2 == 1
                ? latencies[n / 2]
                : (latencies[n / 2 - 1] + latencies[n / 2]) / 2.0;
        long p99 = latencies[(int) Math.ceil(0.99 * n) - 1];

        System.out.println("Threads:             " + threads);
        System.out.println("Successful requests: " + successful.get());
        System.out.println("Failed requests:     " + failed.get());
        System.out.println("Wall time:           " + wallMs + " ms");
        System.out.printf("Throughput:          %.1f req/s%n", throughput);
        System.out.printf("Mean latency:        %.2f ms%n", mean);
        System.out.printf("Median latency:      %.1f ms%n", median);
        System.out.println("p99 latency:         " + p99 + " ms");
        System.out.println("Min latency:         " + latencies[0] + " ms");
        System.out.println("Max latency:         " + latencies[n - 1] + " ms");
        System.out.println("CSV:                 " + csv.toAbsolutePath());
    }
}