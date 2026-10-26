package com.onlinestore.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {

    private static final int TOTAL_REQUESTS = 200_000;
    private static final int QUEUE_CAPACITY = 1_000;

    public static void main(String[] args) throws InterruptedException {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 32;
        URI uri = URI.create(baseUrl + "/api/product");

        BlockingQueue<Product> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        AtomicInteger successful = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        long start = System.currentTimeMillis();

        Thread producer = new Thread(new ProductProducer(queue, TOTAL_REQUESTS, threads));
        producer.start();

        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(new PostWorker(queue, uri, successful, failed));
            worker.start();
            workers.add(worker);
        }

        producer.join();
        for (Thread worker : workers) {
            worker.join();
        }

        long wallMs = System.currentTimeMillis() - start;
        double throughput = TOTAL_REQUESTS / (wallMs / 1000.0);

        System.out.println("Threads:             " + threads);
        System.out.println("Successful requests: " + successful.get());
        System.out.println("Failed requests:     " + failed.get());
        System.out.println("Wall time:           " + wallMs + " ms");
        System.out.printf("Throughput:          %.1f req/s%n", throughput);
        System.out.printf("Expected (linear):   %.1f req/s%n", threads * 24.6);
    }
}