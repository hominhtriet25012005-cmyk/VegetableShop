package com.vegetableshop.dto;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Query parameters accepted by the public shop page.
 */
public class ProductFilter {

    private static final int DEFAULT_PAGE_SIZE = 9;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(3, 6, 9, 12);
    private static final Set<String> ALLOWED_SORTS = Set.of(
        "newest", "priceAsc", "priceDesc", "nameAsc"
    );

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sort = "newest";
    private int page;
    private int size = DEFAULT_PAGE_SIZE;

    public void normalize() {
        keyword = normalizeKeyword(keyword);
        categoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        minPrice = normalizePrice(minPrice);
        maxPrice = normalizePrice(maxPrice);

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal originalMin = minPrice;
            minPrice = maxPrice;
            maxPrice = originalMin;
        }

        sort = ALLOWED_SORTS.contains(sort) ? sort : "newest";
        page = Math.max(page, 0);
        size = ALLOWED_PAGE_SIZES.contains(size) ? size : DEFAULT_PAGE_SIZE;
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 100 ? trimmed : trimmed.substring(0, 100);
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        return value != null && value.signum() >= 0 ? value : null;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
