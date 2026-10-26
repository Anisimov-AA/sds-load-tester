package com.onlinestore.client;

public record LatencyRecord(long startMs, String type, long latencyMs, int statusCode) {

    public String toCsv() {
        return startMs + "," + type + "," + latencyMs + "," + statusCode;
    }
}