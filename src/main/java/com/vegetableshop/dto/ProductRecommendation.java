package com.vegetableshop.dto;

import com.vegetableshop.entity.Product;

public record ProductRecommendation(Product product, int score, String reason) {
}
