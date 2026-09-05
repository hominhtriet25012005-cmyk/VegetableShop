package com.vegetableshop.service;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Brand;
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
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

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
    void findSameBrandProductsUsesActiveBrandAndExcludesCurrentProduct() {
        Brand brand = new Brand();
        brand.setId(8L);
        brand.setStatus(true);
        Product product = new Product();
        product.setId(5L);
        product.setBrand(brand);
        Product related = new Product();
        when(productRepository
            .findTop4ByStatusTrueAndCategoryStatusTrueAndBrandIdAndIdNotOrderByCreatedAtDesc(8L, 5L))
            .thenReturn(List.of(related));

        List<Product> result = new ProductService(productRepository).findSameBrandProducts(product);

        assertEquals(List.of(related), result);
    }

    @Test
    void galleryImagesKeepsMainImageFirstAndRemovesDuplicates() {
        Product product = new Product();
        product.setImage(" /img/main.jpg ");
        product.replaceAdditionalImages(List.of("/img/second.jpg", "/img/main.jpg", "/img/third.jpg"));

        assertEquals(List.of("/img/main.jpg", "/img/second.jpg", "/img/third.jpg"),
            new ProductService(productRepository).galleryImages(product));
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
        newestVegetable.setCategory(vegetables);
        Product mushroom = new Product();
        mushroom.setId(41L);
        mushroom.setCategory(mushrooms);

        when(productRepository.findByStatusTrueAndCategoryStatusTrue())
            .thenReturn(List.of(newestVegetable, mushroom, newestVegetable));

        List<Product> result = new ProductService(productRepository)
            .findHomepageCatalog(List.of(vegetables, mushrooms));

        assertEquals(2, result.size());
        assertEquals(Set.of(newestVegetable, mushroom), Set.copyOf(result));
        verify(productRepository).findByStatusTrueAndCategoryStatusTrue();
    }

    @Test
    void homepageShowsTwoProductsFromEachOfFourCategoriesInFirstEight() {
        List<Category> categories = List.of(category(1), category(2), category(3), category(4));
        List<Product> candidates = new ArrayList<>();
        categories.forEach(category -> candidates.addAll(products(category, 20)));
        List<Product> originalOrder = List.copyOf(candidates);

        List<Product> result = ProductService.mixHomepageCatalog(candidates, categories, new Random(21));

        assertEquals(32, result.size());
        assertEquals(32, result.stream().map(Product::getId).distinct().count());
        for (Category category : categories) {
            assertEquals(2, result.subList(0, 8).stream()
                .filter(product -> product.getCategory().getId().equals(category.getId())).count());
            assertEquals(8, result.stream()
                .filter(product -> product.getCategory().getId().equals(category.getId())).count());
        }
        assertEquals(originalOrder, candidates, "Shuffling must not mutate the source list");
    }

    @Test
    void homepageRandomSampleCanIncludeOlderProductsAndChangesWithSeed() {
        Category category = category(1);
        List<Product> candidates = products(category, 20);
        List<Product> first = ProductService.mixHomepageCatalog(candidates, List.of(category), new Random(1));
        List<Product> second = ProductService.mixHomepageCatalog(candidates, List.of(category), new Random(2));

        assertEquals(8, first.size());
        assertNotEquals(first, second);
        assertTrue(first.stream().anyMatch(product -> !candidates.subList(0, 8).contains(product)),
            "The sample must not be limited to the eight newest products");
    }

    @Test
    void homepageSkipsHiddenProductsAndHiddenOrUnlistedCategories() {
        Category active = category(1);
        Category hidden = category(2);
        hidden.setStatus(false);
        Category unlisted = category(3);
        List<Product> candidates = new ArrayList<>(products(active, 2));
        Product visible = candidates.getFirst();
        candidates.get(1).setStatus(false);
        candidates.addAll(products(hidden, 2));
        candidates.addAll(products(unlisted, 2));

        assertEquals(List.of(visible), ProductService.mixHomepageCatalog(
            candidates, List.of(active, hidden), new Random(1)));
    }

    @Test
    void homepageFillsRemainingSlotsWhenSomeCategoriesAreSparseOrEmpty() {
        Category sparse = category(1);
        Category full = category(2);
        Category empty = category(3);
        List<Product> candidates = new ArrayList<>(products(sparse, 1));
        candidates.addAll(products(full, 20));

        List<Product> result = ProductService.mixHomepageCatalog(
            candidates, List.of(sparse, full, empty), new Random(3));

        assertEquals(9, result.size());
        assertEquals(2, result.subList(0, 2).stream().map(Product::getCategory).distinct().count());
        assertEquals(7, result.subList(0, 8).stream()
            .filter(product -> product.getCategory() == full).count());
        assertEquals(9, Set.copyOf(result).size());
    }

    @Test
    void homepageReturnsEmptyWithoutQueryWhenThereAreNoCategories() {
        assertTrue(new ProductService(productRepository).findHomepageCatalog(List.of()).isEmpty());
        verifyNoInteractions(productRepository);
        assertTrue(ProductService.mixHomepageCatalog(List.of(), List.of(category(1)), new Random(1)).isEmpty());
    }

    private static Category category(long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private static List<Product> products(Category category, int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(index -> {
            Product product = new Product();
            product.setId(category.getId() * 100 + index);
            product.setCategory(category);
            return product;
        }).collect(Collectors.toList());
    }
}
