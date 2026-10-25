package com.onlinestore.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SingleThreadClient {

    private static final int REQUESTS = 10_000;

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        URI uri = URI.create(baseUrl + "/api/product");

        HttpClient http = HttpClient.newHttpClient();
        ObjectMapper json = new ObjectMapper();
        ProductGenerator generator = new ProductGenerator();

        int ok = 0;
        int failed = 0;
        long start = System.currentTimeMillis();

        for (int i = 0; i < REQUESTS; i++) {
            String body = json.writeValueAsString(generator.next());
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                ok++;
            } else {
                failed++;
            }
        }

        long wallMs = System.currentTimeMillis() - start;
        double throughput = REQUESTS / (wallMs / 1000.0);

        System.out.println("Successful requests: " + ok);
        System.out.println("Failed requests:     " + failed);
        System.out.println("Wall time:           " + wallMs + " ms");
        System.out.printf("Avg latency:         %.2f ms%n", (double) wallMs / REQUESTS);
        System.out.printf("Throughput:          %.1f req/s%n", throughput);
    }
}