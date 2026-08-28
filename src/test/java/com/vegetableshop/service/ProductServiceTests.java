package com.vegetableshop.service;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Test
    @SuppressWarnings("unchecked")
    void searchNormalizesFilterAndUsesRequestedSortAndPagination() {
        Product product = new Product();
        ProductFilter filter = new ProductFilter();
        filter.setKeyword("  Cam  ");
        filter.setPage(-2);
        filter.setSize(6);
        filter.setSort("priceAsc");
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(product)));

        ProductService productService = new ProductService(productRepository);
        Page<Product> result = productService.search(filter);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals("Cam", filter.getKeyword());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(6, pageableCaptor.getValue().getPageSize());
        assertEquals("price: ASC,id: ASC", pageableCaptor.getValue().getSort().toString());
        assertSame(product, result.getContent().getFirst());
    }

    @Test
    void findActiveProductReturnsProductWithCategory() {
        Product product = new Product();
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(7L))
            .thenReturn(Optional.of(product));

        Product actual = new ProductService(productRepository).findActiveProduct(7L);

        assertSame(product, actual);
    }

    @Test
    void findActiveProductThrowsFriendlyExceptionWhenMissing() {
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            ProductNotFoundException.class,
            () -> new ProductService(productRepository).findActiveProduct(99L)
        );
    }

    @Test
    void findRelatedProductsUsesCategoryAndExcludesCurrentProduct() {
        Category category = new Category();
        category.setId(2L);
        Product product = new Product();
        product.setId(5L);
        product.setCategory(category);
        Product related = new Product();
        when(productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(2L, 5L))
            .thenReturn(List.of(related));

        List<Product> result = new ProductService(productRepository).findRelatedProducts(product);

        assertEquals(List.of(related), result);
    }

    @Test
    void findHomepageProductsReturnsNewestEightActiveProducts() {
        Product newest = new Product();
        when(productRepository.findTop8ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc())
            .thenReturn(List.of(newest));

        List<Product> result = new ProductService(productRepository).findHomepageProducts();

        assertEquals(List.of(newest), result);
    }

    @Test
    void findHomepageCatalogIncludesEachCategoryAndRemovesDuplicates() {
        Category vegetables = new Category();
        vegetables.setId(1L);
        Category mushrooms = new Category();
        mushrooms.setId(4L);

        Product newestVegetable = new Product();
        newestVegetable.setId(11L);
        Product mushroom = new Product();
        mushroom.setId(41L);

        when(productRepository.findTop8ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc())
            .thenReturn(List.of(newestVegetable));
        when(productRepository.findTop8ByStatusTrueAndCategoryStatusTrueAndCategoryIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(newestVegetable));
        when(productRepository.findTop8ByStatusTrueAndCategoryStatusTrueAndCategoryIdOrderByCreatedAtDesc(4L))
            .thenReturn(List.of(mushroom));

        List<Product> result = new ProductService(productRepository)
            .findHomepageCatalog(List.of(vegetables, mushrooms));

        assertEquals(List.of(newestVegetable, mushroom), result);
    }
}
