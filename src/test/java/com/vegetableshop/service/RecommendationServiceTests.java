package com.vegetableshop.service;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTests {

    @Mock ProductService productService;

    @Test
    void sameCategoryAndBrandRanksFirstAndUnavailableProductsAreExcluded() {
        Category vegetables = category(1L);
        Category fruit = category(2L);
        Brand brandA = brand(1L);
        Brand brandB = brand(2L);
        Product current = product(1L, vegetables, brandA, "100000", 10, true);
        Product best = product(2L, vegetables, brandA, "110000", 10, true);
        Product nearPrice = product(3L, fruit, brandB, "105000", 10, true);
        Product unavailable = product(4L, vegetables, brandA, "100000", 0, true);
        Product inactive = product(5L, vegetables, brandA, "100000", 10, false);
        when(productService.findAllActiveProducts())
            .thenReturn(List.of(current, nearPrice, unavailable, inactive, best));

        var recommendations = new RecommendationService(productService).recommend(current, List.of());

        assertEquals(best, recommendations.getFirst().product());
        assertEquals("Cùng danh mục và thương hiệu", recommendations.getFirst().reason());
        assertFalse(recommendations.stream().anyMatch(item -> item.product().equals(unavailable)));
        assertFalse(recommendations.stream().anyMatch(item -> item.product().equals(inactive)));
    }

    private Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setStatus(true);
        return category;
    }

    private Brand brand(Long id) {
        Brand brand = new Brand();
        brand.setId(id);
        brand.setStatus(true);
        return brand;
    }

    private Product product(
        Long id,
        Category category,
        Brand brand,
        String price,
        int quantity,
        boolean active
    ) {
        Product product = new Product();
        product.setId(id);
        product.setCategory(category);
        product.setBrand(brand);
        product.setPrice(new BigDecimal(price));
        product.setQuantity(quantity);
        product.setStatus(active);
        return product;
    }
}
