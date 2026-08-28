package com.vegetableshop.dto;

import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardView(
    long productCount,
    long categoryCount,
    long userCount,
    long orderCount,
    BigDecimal revenue,
    List<Order> recentOrders,
    List<Product> lowStockProducts
) {
}
