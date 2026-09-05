package com.vegetableshop.event;

import com.vegetableshop.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public record OrderPlacedMailEvent(
    String email,
    String fullName,
    String orderCode,
    String receiverName,
    String receiverPhone,
    String shippingAddress,
    BigDecimal totalAmount,
    List<OrderLineMailData> items
) {
    public static OrderPlacedMailEvent from(Order order) {
        List<OrderLineMailData> lines = order.getDetails().stream()
            .map(detail -> new OrderLineMailData(
                detail.getProductName(),
                detail.getQuantity(),
                detail.getPrice(),
                detail.getSubtotal()
            ))
            .toList();
        return new OrderPlacedMailEvent(
            order.getUser().getEmail(),
            order.getUser().getFullName(),
            order.getOrderCode(),
            order.getReceiverName(),
            order.getReceiverPhone(),
            order.getShippingAddress(),
            order.getTotalAmount(),
            List.copyOf(lines)
        );
    }
}
