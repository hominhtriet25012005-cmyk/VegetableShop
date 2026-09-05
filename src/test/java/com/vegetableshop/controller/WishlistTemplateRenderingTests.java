package com.vegetableshop.controller;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Wishlist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(WishlistTemplateRenderingTests.WishlistPreviewController.class)
class WishlistTemplateRenderingTests {

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void wishlistRendersProductAndAjaxControls() throws Exception {
        mockMvc.perform(get("/__test/wishlist"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Sản phẩm yêu thích")))
            .andExpect(content().string(containsString("Gạo lứt sấy rong biển")))
            .andExpect(content().string(containsString("data-wishlist-move")))
            .andExpect(content().string(containsString("data-wishlist-toggle")))
            .andExpect(content().string(containsString("src=\"/js/wishlist.js\"")));
    }

    @Controller
    static class WishlistPreviewController {

        @GetMapping("/__test/wishlist")
        String preview(Model model) {
            Category category = new Category();
            category.setId(1L);
            category.setName("Đồ khô & Hạt dinh dưỡng");
            category.setStatus(true);

            Product product = new Product();
            product.setId(5L);
            product.setName("Gạo lứt sấy rong biển");
            product.setPrice(new BigDecimal("65000"));
            product.setQuantity(12);
            product.setCategory(category);
            product.setStatus(true);

            Wishlist wishlist = new Wishlist();
            wishlist.setProduct(product);
            model.addAttribute("wishlistItems", List.of(wishlist));
            return "wishlist";
        }
    }
}
