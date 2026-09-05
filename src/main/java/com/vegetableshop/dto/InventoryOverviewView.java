package com.vegetableshop.dto;

import java.util.List;

public record InventoryOverviewView(
    long totalInventoryUnits,
    long lowStockProductCount,
    long outOfStockProductCount,
    List<ProductRow> products,
    int pageNumber,
    int totalPages
) {
    public InventoryOverviewView(
        long totalInventoryUnits,
        long lowStockProductCount,
        long outOfStockProductCount,
        List<ProductRow> products
    ) {
        this(totalInventoryUnits, lowStockProductCount, outOfStockProductCount, products, 0, 1);
    }

    public boolean hasPreviousPage() {
        return pageNumber > 0;
    }

    public boolean hasNextPage() {
        return pageNumber + 1 < totalPages;
    }

    public record ProductRow(
        Long productId,
        String productName,
        String categoryName,
        String brandName,
        int quantity,
        int lowStockThreshold,
        boolean active
    ) {
        public boolean outOfStock() {
            return quantity <= 0;
        }

        public boolean lowStock() {
            return quantity > 0 && quantity <= lowStockThreshold;
        }

        public boolean healthyStock() {
            return quantity > lowStockThreshold;
        }
    }
}
