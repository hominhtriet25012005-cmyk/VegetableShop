package com.vegetableshop.service;

import com.vegetableshop.entity.DiscountType;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Promotion;
import com.vegetableshop.repository.PromotionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionPricingServiceTests {

    @Test
    void overlappingPromotionsChooseLowestPrice() {
        PromotionRepository repository = mock(PromotionRepository.class);
        Product product = product(7L, "100000");
        Promotion tenPercent = promotion("Giảm 10%", DiscountType.PERCENTAGE, "10", false, product);
        Promotion fixed = promotion("Flash giảm 15000", DiscountType.FIXED_AMOUNT, "15000", true, product);
        when(repository.findByStatusTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(any(), any()))
            .thenReturn(List.of(tenPercent, fixed));

        var result = new PromotionPricingService(repository)
            .prices(List.of(product), LocalDateTime.now()).get(product.getId());

        assertEquals(new BigDecimal("85000.00"), result.effective());
        assertEquals(new BigDecimal("15000.00"), result.unitDiscount());
        assertEquals("Flash giảm 15000", result.promotionName());
        assertTrue(result.flashSale());
    }

    @Test
    void fixedDiscountNeverMakesPriceNegative() {
        assertEquals(
            new BigDecimal("25000.00"),
            PromotionPricingService.discount(
                new BigDecimal("25000"), DiscountType.FIXED_AMOUNT, new BigDecimal("50000")
            )
        );
    }

    private Product product(Long id, String price) {
        Product product = new Product();
        product.setId(id);
        product.setPrice(new BigDecimal(price));
        return product;
    }

    private Promotion promotion(String name, DiscountType type, String value, boolean flashSale, Product product) {
        Promotion promotion = new Promotion();
        promotion.setName(name);
        promotion.setDiscountType(type);
        promotion.setDiscountValue(new BigDecimal(value));
        promotion.setFlashSale(flashSale);
        promotion.replaceProducts(List.of(product));
        return promotion;
    }
}
