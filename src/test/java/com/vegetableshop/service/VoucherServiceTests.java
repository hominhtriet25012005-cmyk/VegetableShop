package com.vegetableshop.service;

import com.vegetableshop.entity.*;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.repository.VoucherRepository;
import com.vegetableshop.repository.VoucherUsageRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoucherServiceTests {

    @Test
    void categoryVoucherOnlyDiscountsEligibleLinesAndHonorsCap() {
        VoucherRepository vouchers = mock(VoucherRepository.class);
        VoucherUsageRepository usages = mock(VoucherUsageRepository.class);
        Voucher voucher = voucher(1L, VoucherScopeType.CATEGORY, 10L);
        voucher.setMaximumDiscountAmount(new BigDecimal("12000"));
        when(vouchers.findByCodeIgnoreCase("RAU20")).thenReturn(Optional.of(voucher));

        User user = new User();
        user.setId(5L);
        Product vegetable = product(1L, 10L);
        Product fruit = product(2L, 11L);
        var lines = new LinkedHashMap<Product, BigDecimal>();
        lines.put(vegetable, new BigDecimal("80000"));
        lines.put(fruit, new BigDecimal("70000"));

        var result = new VoucherService(vouchers, usages)
            .evaluate("RAU20", user, lines, new BigDecimal("150000"));

        assertEquals(new BigDecimal("12000.00"), result.discount());
        assertEquals(java.util.Set.of(1L), result.eligibleProductIds());
    }

    @Test
    void rejectsVoucherAfterUserLimitReached() {
        VoucherRepository vouchers = mock(VoucherRepository.class);
        VoucherUsageRepository usages = mock(VoucherUsageRepository.class);
        Voucher voucher = voucher(1L, VoucherScopeType.ORDER, null);
        when(vouchers.findByCodeIgnoreCase("RAU20")).thenReturn(Optional.of(voucher));
        when(usages.countByVoucherIdAndUserIdAndActiveTrue(1L, 5L)).thenReturn(1L);
        User user = new User();
        user.setId(5L);
        Product product = product(1L, 10L);

        assertThrows(OrderOperationException.class, () -> new VoucherService(vouchers, usages)
            .evaluate("RAU20", user, java.util.Map.of(product, new BigDecimal("100000")), new BigDecimal("100000")));
    }

    private Voucher voucher(Long id, VoucherScopeType scope, Long targetId) {
        Voucher voucher = new Voucher();
        voucher.setId(id);
        voucher.setCode("RAU20");
        voucher.setStatus(true);
        voucher.setDiscountType(DiscountType.PERCENTAGE);
        voucher.setDiscountValue(new BigDecimal("20"));
        voucher.setMinimumOrderAmount(BigDecimal.ZERO);
        voucher.setStartsAt(LocalDateTime.now().minusDays(1));
        voucher.setEndsAt(LocalDateTime.now().plusDays(1));
        voucher.setPerUserUsageLimit(1);
        voucher.replaceScopes(scope, targetId == null ? List.of() : List.of(targetId));
        return voucher;
    }

    private Product product(Long id, Long categoryId) {
        Category category = new Category();
        category.setId(categoryId);
        Product product = new Product();
        product.setId(id);
        product.setCategory(category);
        return product;
    }
}
