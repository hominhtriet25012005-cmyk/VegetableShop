package com.vegetableshop.dto;

public record ReviewSummary(double averageRating, long reviewCount, java.util.Map<Integer, Long> starCounts) {
    public ReviewSummary(double averageRating, long reviewCount) { this(averageRating, reviewCount, java.util.Map.of()); }
    public long countFor(int star) { return starCounts.getOrDefault(star, 0L); }
    public int percentFor(int star) { return reviewCount == 0 ? 0 : (int) Math.round(countFor(star) * 100.0 / reviewCount); }
    public static ReviewSummary empty() {
        return new ReviewSummary(0.0, 0L);
    }
}
