package com.vegetableshop.dto;

public record ReviewSummary(double averageRating, long reviewCount) {
    public static ReviewSummary empty() {
        return new ReviewSummary(0.0, 0L);
    }
}
