package com.vegetableshop.dto;

import java.math.BigDecimal;

public record ChatbotProductView(
    Long id,
    String name,
    BigDecimal price,
    String image,
    String category,
    String unit,
    int availableQuantity,
    String detailUrl
) {
}
