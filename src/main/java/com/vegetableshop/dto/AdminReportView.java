package com.vegetableshop.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Immutable data model for the Phase 15 business report.
 */
public record AdminReportView(
    LocalDate fromDate,
    LocalDate toDate,
    BigDecimal totalRevenue,
    long completedOrderCount,
    long unitsSold,
    BigDecimal averageOrderValue,
    long totalInventoryUnits,
    long lowStockProductCount,
    long outOfStockProductCount,
    int lowStockThreshold,
    List<InventoryRow> inventory,
    List<SalesRow> productSales,
    List<SalesRow> categorySales,
    List<SalesRow> brandSales,
    List<SalesRow> supplierPurchases,
    List<SalesRow> customerSales,
    List<RevenuePoint> monthlyRevenue,
    List<RevenuePoint> quarterlyRevenue,
    List<RevenuePoint> yearlyRevenue
) {
    public record InventoryRow(
        Long productId,
        String productName,
        String categoryName,
        String brandName,
        int quantity,
        int lowStockThreshold,
        boolean active
    ) {
        public boolean outOfStock() {
            return quantity == 0;
        }

        public boolean lowStock() {
            return quantity > 0 && quantity <= lowStockThreshold;
        }
    }

    public record SalesRow(String label, long units, BigDecimal revenue) {
    }

    public record RevenuePoint(String label, BigDecimal revenue) {
    }
}
