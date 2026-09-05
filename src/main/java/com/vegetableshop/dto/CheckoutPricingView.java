package com.vegetableshop.dto;

import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Voucher;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutPricingView(
    List<Line> lines,
    BigDecimal subtotalAmount,
    BigDecimal promotionDiscountAmount,
    BigDecimal voucherDiscountAmount,
    BigDecimal totalAmount,
    Voucher voucher,
    String voucherMessage
) {
    public record Line(Product product, int quantity, BigDecimal originalUnitPrice,
                       BigDecimal effectiveUnitPrice, BigDecimal promotionDiscountAmount,
                       BigDecimal voucherDiscountAmount, BigDecimal totalAmount,
                       String promotionName, boolean flashSale) {
        public BigDecimal originalTotalAmount() {
            return originalUnitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
