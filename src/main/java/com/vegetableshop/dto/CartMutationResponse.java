package com.vegetableshop.dto;

import java.math.BigDecimal;

public record CartMutationResponse(
    Long itemId,
    int quantity,
    int stock,
    BigDecimal itemSubtotal,
    int totalQuantity,
    BigDecimal cartTotal,
    boolean empty,
    String message
) {
}
