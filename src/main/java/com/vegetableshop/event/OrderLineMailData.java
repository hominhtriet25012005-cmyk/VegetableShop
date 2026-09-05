package com.vegetableshop.event;

import java.math.BigDecimal;

public record OrderLineMailData(String productName, int quantity, BigDecimal price, BigDecimal subtotal) {
}
