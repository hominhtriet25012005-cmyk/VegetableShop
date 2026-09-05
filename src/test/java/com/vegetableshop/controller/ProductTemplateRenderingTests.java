package com.vegetableshop.controller;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.dto.ReviewSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(ProductTemplateRenderingTests.ProductPreviewController.class)
class ProductTemplateRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shopTemplateRendersFilterWithoutDatabase() throws Exception {
        mockMvc.perform(get("/shop").param("keyword", "Cam"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Cam\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-header-search-form")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"keyword\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"submit\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tìm thấy")));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void shopPaginationRendersOnlyFiveNearbyPageNumbers() throws Exception {
        mockMvc.perform(get("/__test/shop-pagination"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("35.000 ₫ / g")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Còn hàng")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                "href=\"/shop?page=1&amp;size=9"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "href=\"/shop?page=2&amp;size=9")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "href=\"/shop?page=6&amp;size=9")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                "href=\"/shop?page=7&amp;size=9"))));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void productDetailTemplateRendersDynamicProduct() throws Exception {
        mockMvc.perform(get("/__test/product/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Bông cải kiểm thử")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("35.000")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("35.000 ₫ / g")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>10 g</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SKU: RAU-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nông Sản Việt")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đà Lạt, Việt Nam")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Đường dẫn sản phẩm\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/shop?categoryId=1\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("product-detail.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-gallery-thumb")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Mua ngay")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Hàng cùng loại")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Sản phẩm cùng thương hiệu")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nhà cung cấp"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đánh giá từ khách hàng")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-header-search-form")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cart/items\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Controller
    static class ProductPreviewController {

        @GetMapping("/__test/product/{id}")
        String preview(@PathVariable Long id, Model model) {
            Category category = new Category();
            category.setId(1L);
            category.setName("Rau củ");

            Product product = product(id, "Bông cải kiểm thử", category);
            Product relatedProduct = product(2L, "Ớt chuông kiểm thử", category);

            model.addAttribute("product", product);
            model.addAttribute("productImages", List.of(product.getImage(), "/img/vegetable-item-2.jpg"));
            model.addAttribute("relatedProducts", List.of(relatedProduct));
            model.addAttribute("sameBrandProducts", List.of(relatedProduct));
            model.addAttribute("recentlyViewedProducts", List.of(relatedProduct));
            model.addAttribute("featuredProducts", List.of(product));
            model.addAttribute("categories", List.of(category));
            model.addAttribute("reviews", List.of());
            model.addAttribute("reviewSummary", ReviewSummary.empty());
            model.addAttribute("canReview", false);
            model.addAttribute("reviewRequest", new ReviewRequest());
            model.addAttribute("wishlisted", false);
            model.addAttribute("recommendedProducts", List.of(new com.vegetableshop.dto.ProductRecommendation(relatedProduct, 1, "Cùng danh mục")));
            model.addAttribute("wishlistProductIds", java.util.Set.of(2L));
            return "shop-detail";
        }

        @GetMapping("/__test/shop-pagination")
        String shopPagination(Model model) {
            Category category = new Category();
            category.setId(1L);
            category.setName("Rau củ");

            Product product = product(1L, "Sản phẩm phân trang", category);
            ProductFilter filter = new ProductFilter();
            filter.setPage(4);
            filter.normalize();

            model.addAttribute("filter", filter);
            model.addAttribute("productPage",
                new PageImpl<>(List.of(product), PageRequest.of(4, filter.getSize()), 100));
            model.addAttribute("products", List.of(product));
            model.addAttribute("featuredProducts", List.of(product));
            model.addAttribute("categories", List.of(category));
            return "shop";
        }

        private Product product(Long id, String name, Category category) {
            Product product = new Product();
            product.setId(id);
            product.setSku("RAU-001");
            product.setName(name);
            product.setDescription("Mô tả sản phẩm kiểm thử");
            product.setPrice(new BigDecimal("35000"));
            product.setQuantity(10);
            product.setImage("/img/vegetable-item-1.jpg");
            product.setCategory(category);
            Brand brand = new Brand();
            brand.setId(1L);
            brand.setName("Nông Sản Việt");
            brand.setStatus(true);
            product.setBrand(brand);
            product.setUnit(ProductUnit.GRAM);
            product.setOrigin("Đà Lạt, Việt Nam");
            return product;
        }
    }
}
