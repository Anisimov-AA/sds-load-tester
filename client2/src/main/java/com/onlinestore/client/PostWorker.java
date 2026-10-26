package com.onlinestore.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;                                   // NEW
import java.util.List;                                        // NEW
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PostWorker implements Runnable {

    static final Product POISON = new Product(0, "", "", 0, 0, 0);
    private static final int MAX_RETRIES = 5;

    private final BlockingQueue<Product> queue;
    private final URI uri;
    private final AtomicInteger successful;
    private final AtomicInteger failed;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final List<LatencyRecord> records = new ArrayList<>();   // NEW: own notebook

    public PostWorker(BlockingQueue<Product> queue, URI uri,
                      AtomicInteger successful, AtomicInteger failed) {
        this.queue = queue;
        this.uri = uri;
        this.successful = successful;
        this.failed = failed;
    }

    public List<LatencyRecord> getRecords() {                         // NEW
        return records;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Product product = queue.take();
                if (product == POISON) {
                    return;
                }
                if (send(product)) {
                    successful.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean send(Product product) throws InterruptedException {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(product)))
                    .build();
        } catch (Exception e) {
            return false;
        }

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            long start = System.currentTimeMillis();                   // NEW: clock before
            int status;
            try {
                status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            } catch (java.io.IOException e) {
                status = -1;                                           // NEW: -1 = network error
            }
            long latency = System.currentTimeMillis() - start;         // NEW: clock after
            records.add(new LatencyRecord(start, "POST", latency, status));   // NEW: write a line

            if (status == 201) {
                return true;
            }
        }
        return false;
    }
}