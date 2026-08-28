package com.vegetableshop.controller;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.dto.ReviewSummary;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.service.CategoryService;
import com.vegetableshop.service.ProductService;
import com.vegetableshop.service.RecentlyViewedService;
import com.vegetableshop.service.ReviewService;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTests {

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private RecentlyViewedService recentlyViewedService;

    @Test
    void shopAddsFilteredPageAndSupportingDataToModel() {
        Product product = new Product();
        Category category = new Category();
        ProductFilter filter = new ProductFilter();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 3), 1);
        when(productService.search(filter)).thenReturn(page);
        when(productService.findNewestProducts()).thenReturn(List.of(product));
        when(categoryService.findAllActiveCategories()).thenReturn(List.of(category));

        ProductController controller = controller();
        Model model = new ConcurrentModel();

        String view = controller.shop(filter, model);

        assertEquals("shop", view);
        assertSame(page, model.getAttribute("productPage"));
        assertEquals(List.of(product), model.getAttribute("products"));
        assertEquals(List.of(product), model.getAttribute("featuredProducts"));
        assertEquals(List.of(category), model.getAttribute("categories"));
    }

    @Test
    void detailAddsProductRelatedProductsAndCategoriesToModel() {
        Product product = new Product();
        Product related = new Product();
        Category category = new Category();
        when(productService.findActiveProduct(3L)).thenReturn(product);
        when(productService.findRelatedProducts(product)).thenReturn(List.of(related));
        when(productService.findNewestProducts()).thenReturn(List.of(product));
        when(categoryService.findAllActiveCategories()).thenReturn(List.of(category));
        when(reviewService.summarize(3L)).thenReturn(ReviewSummary.empty());
        when(recentlyViewedService.recordAndGetPrevious(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(3L))).thenReturn(List.of());

        ProductController controller = controller();
        Model model = new ConcurrentModel();

        String view = controller.detail(3L, null, new MockHttpSession(), model);

        assertEquals("shop-detail", view);
        assertSame(product, model.getAttribute("product"));
        assertEquals(List.of(related), model.getAttribute("relatedProducts"));
        assertEquals(List.of(category), model.getAttribute("categories"));
        assertEquals(ReviewSummary.empty(), model.getAttribute("reviewSummary"));
    }

    private ProductController controller() {
        return new ProductController(productService, categoryService, reviewService, recentlyViewedService);
    }
}
