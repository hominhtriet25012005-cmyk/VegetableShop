package com.vegetableshop.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductFilterTests {

    @Test
    void normalizeKeepsSafeValuesAndSwapsReversedPriceRange() {
        ProductFilter filter = new ProductFilter();
        filter.setKeyword("  Bông cải  ");
        filter.setCategoryId(-1L);
        filter.setMinPrice(new BigDecimal("80000"));
        filter.setMaxPrice(new BigDecimal("30000"));
        filter.setSort("unsupported");
        filter.setPage(-5);
        filter.setSize(1000);

        filter.normalize();

        assertEquals("Bông cải", filter.getKeyword());
        assertNull(filter.getCategoryId());
        assertEquals(new BigDecimal("30000"), filter.getMinPrice());
        assertEquals(new BigDecimal("80000"), filter.getMaxPrice());
        assertEquals("newest", filter.getSort());
        assertEquals(0, filter.getPage());
        assertEquals(9, filter.getSize());
    }
}
