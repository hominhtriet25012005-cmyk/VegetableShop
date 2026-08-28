package com.vegetableshop.controller;

import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(CartTemplateRenderingTests.CartPreviewController.class)
class CartTemplateRenderingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cartTemplateRendersDatabaseValuesAndSecureForms() throws Exception {
        mockMvc.perform(get("/__test/cart-preview"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cam kiểm thử")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("70.000 ₫")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/cart/items/11/quantity")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-cart-update")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/cart.js")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("sweetalert2@11")));
    }

    @Test
    void cartJavaScriptCapturesFormDataBeforeDisablingControls() throws Exception {
        String script = new ClassPathResource("static/js/cart.js")
            .getContentAsString(StandardCharsets.UTF_8);
        int addHandler = script.indexOf("if (addForm)");
        int captureBody = script.indexOf("new URLSearchParams(new FormData(addForm))", addHandler);
        int disableForm = script.indexOf("setBusy(addForm, true)", addHandler);

        assertTrue(addHandler >= 0, "Cart add handler must exist");
        assertTrue(captureBody > addHandler, "Cart form data must be captured");
        assertTrue(captureBody < disableForm,
            "productId and quantity must be captured before the form controls are disabled");
    }

    @Controller
    static class CartPreviewController {

        @GetMapping("/__test/cart-preview")
        String preview(Model model) {
            Product product = new Product();
            product.setId(3L);
            product.setName("Cam kiểm thử");
            product.setImage("/img/fruite-item-4.jpg");
            product.setPrice(new BigDecimal("35000"));
            product.setQuantity(10);

            CartItem item = new CartItem();
            item.setId(11L);
            item.setProduct(product);
            item.setQuantity(2);

            model.addAttribute("cartItems", List.of(item));
            model.addAttribute("cartTotal", new BigDecimal("70000"));
            model.addAttribute("cartItemCount", 2);
            return "cart";
        }
    }
}
