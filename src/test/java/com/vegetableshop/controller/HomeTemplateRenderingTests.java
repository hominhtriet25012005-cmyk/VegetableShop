package com.vegetableshop.controller;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(HomeTemplateRenderingTests.HomePreviewController.class)
class HomeTemplateRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void homepageRendersVietnameseDynamicCatalogLinks() throws Exception {
        mockMvc.perform(get("/__test/home"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("lang=\"vi\"")))
            .andExpect(content().string(containsString("Sản phẩm hữu cơ của chúng tôi")))
            .andExpect(content().string(containsString("href=\"/shop?categoryId=1\"")))
            .andExpect(content().string(containsString("data-home-category-filter")))
            .andExpect(content().string(containsString("data-category-id=\"1\"")))
            .andExpect(content().string(containsString("data-home-product-wrapper")))
            .andExpect(content().string(containsString("href=\"/product/11\"")))
            .andExpect(content().string(containsString("action=\"/cart/items\"")))
            .andExpect(content().string(containsString("src=\"/js/home-products.js\"")))
            .andExpect(content().string(containsString("data-home-product-card")))
            .andExpect(content().string(containsString("href=\"/news\"")))
            .andExpect(content().string(containsString(">Tin tức</a>")))
            .andExpect(content().string(containsString(">Sản phẩm</a>")))
            .andExpect(content().string(not(containsString(">Cửa hàng</a>"))))
            .andExpect(content().string(not(containsString(">Sản phẩm mới</a>"))))
            .andExpect(content().string(not(containsString("Our Organic Products"))))
            .andExpect(content().string(not(containsString("Lorem ipsum"))));
    }

    @Test
    void newsPageRendersVietnamesePlaceholderAndActiveMenu() throws Exception {
        mockMvc.perform(get("/news"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<title>Tin tức - Vegetable Shop</title>")))
            .andExpect(content().string(containsString("Nội dung tin tức đang được phát triển")))
                .andExpect(content().string(containsString("href=\"/news\" class=\"nav-item nav-link")))
                .andExpect(content().string(containsString("active\">Tin tức")));
    }

    @Test
    void allPublicCommerceTemplatesUseTheSharedVietnameseMenu() throws Exception {
        List<String> templates = List.of(
            "index.html", "shop.html", "shop-detail.html", "news.html", "contact.html",
            "testimonial.html", "cart.html", "checkout.html", "my-orders.html",
            "order-detail.html", "error/404.html"
        );

        for (String template : templates) {
            String html = new ClassPathResource("templates/" + template)
                .getContentAsString(StandardCharsets.UTF_8);
            assertTrue(html.contains("fragments/navigation :: mainMenu"),
                template + " must use the shared public navigation fragment");
        }
    }

    @Controller
    static class HomePreviewController {

        @GetMapping("/__test/home")
        String preview(Model model) {
            Category category = new Category();
            category.setId(1L);
            category.setName("Rau củ");

            Product product = new Product();
            product.setId(11L);
            product.setName("Cà rốt Đà Lạt");
            product.setDescription("Cà rốt tươi sạch được tuyển chọn mỗi ngày.");
            product.setPrice(new BigDecimal("32000"));
            product.setQuantity(25);
            product.setCategory(category);

            model.addAttribute("homeProducts", List.of(product));
            model.addAttribute("homeFeaturedCount", 1);
            model.addAttribute("categories", List.of(category));
            return "index";
        }
    }
}
