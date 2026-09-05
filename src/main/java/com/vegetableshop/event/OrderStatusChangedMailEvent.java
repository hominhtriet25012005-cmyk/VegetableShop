package com.vegetableshop.event;

import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;

public record OrderStatusChangedMailEvent(
    String email,
    String fullName,
    String orderCode,
    OrderStatus status
) {
    public static OrderStatusChangedMailEvent from(Order order) {
        return new OrderStatusChangedMailEvent(
            order.getUser().getEmail(),
            order.getUser().getFullName(),
            order.getOrderCode(),
            order.getStatus()
        );
    }
}
