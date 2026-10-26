package com.onlinestore.client;

import java.util.concurrent.BlockingQueue;

public class ProductProducer implements Runnable {

    private final BlockingQueue<Product> queue;
    private final int total;
    private final int workers;
    private final ProductGenerator generator = new ProductGenerator();

    public ProductProducer(BlockingQueue<Product> queue, int total, int workers) {
        this.queue = queue;
        this.total = total;
        this.workers = workers;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < total; i++) {
                queue.put(generator.next());
            }
            for (int i = 0; i < workers; i++) {
                queue.put(PostWorker.POISON);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}