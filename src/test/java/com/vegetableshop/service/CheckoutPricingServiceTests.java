package com.vegetableshop.service;

import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckoutPricingServiceTests {

    @Test
    void promotionRunsBeforeVoucherAndLineTotalsAddUp() {
        PromotionPricingService promotions = mock(PromotionPricingService.class);
        VoucherService vouchers = mock(VoucherService.class);
        Product first = product(1L, "100000");
        Product second = product(2L, "50000");
        var quantities = new LinkedHashMap<Product, Integer>();
        quantities.put(first, 2);
        quantities.put(second, 1);

        when(promotions.prices(anyCollection(), any(LocalDateTime.class))).thenReturn(Map.of(
            1L, new PromotionPricingService.Price(new BigDecimal("100000"), new BigDecimal("90000.00"),
                new BigDecimal("10000.00"), "Giảm 10%", false),
            2L, new PromotionPricingService.Price(new BigDecimal("50000"), new BigDecimal("50000"),
                BigDecimal.ZERO, null, false)
        ));
        when(vouchers.evaluate(eq("SAVE23"), any(User.class), anyMap(), eq(new BigDecimal("230000.00"))))
            .thenReturn(new VoucherService.Result(null, Set.of(1L, 2L), new BigDecimal("23000.00")));

        var quote = new CheckoutPricingService(promotions, vouchers)
            .quote(quantities, new User(), "SAVE23", false);

        assertEquals(new BigDecimal("250000"), quote.subtotalAmount());
        assertEquals(new BigDecimal("20000.00"), quote.promotionDiscountAmount());
        assertEquals(new BigDecimal("23000.00"), quote.voucherDiscountAmount());
        assertEquals(new BigDecimal("207000.00"), quote.totalAmount());
        assertEquals(quote.totalAmount(), quote.lines().stream()
            .map(line -> line.totalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Product product(Long id, String price) {
        Product product = new Product();
        product.setId(id);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}
