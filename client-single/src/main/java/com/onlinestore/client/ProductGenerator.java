package com.onlinestore.client;

import java.util.concurrent.ThreadLocalRandom;

public class ProductGenerator {

    private static final String SKU_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String[] MANUFACTURERS = {
            "Acme Tools", "Globex", "Initech", "Umbrella Corp", "Stark Industries"
    };

    public Product next() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Product(
                random.nextInt(1, Integer.MAX_VALUE),      // productId
                randomSku(random),                          // sku
                MANUFACTURERS[random.nextInt(MANUFACTURERS.length)],
                random.nextInt(1, 100_001),                 // categoryId: 1..100 000
                random.nextInt(1_000, 10_001),              // weight: 1 000..10 000
                random.nextInt(1, Integer.MAX_VALUE)        // someOtherId
        );
    }

    private String randomSku(ThreadLocalRandom random) {
        StringBuilder sku = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sku.append(SKU_CHARS.charAt(random.nextInt(SKU_CHARS.length())));
        }
        return sku.toString();
    }
}