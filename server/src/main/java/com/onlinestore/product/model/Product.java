package com.onlinestore.product.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Product {

    @JsonProperty("product_id")
    private Integer productId;

    private String sku;

    private String manufacturer;

    @JsonProperty("category_id")
    private Integer categoryId;

    private Integer weight;

    @JsonProperty("some_other_id")
    private Integer someOtherId;
}