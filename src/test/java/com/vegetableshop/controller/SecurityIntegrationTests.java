package com.vegetableshop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng nhập")));

        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo tài khoản khách hàng")));

        mockMvc.perform(get("/news"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tin tức")));
    }

    @Test
    void anonymousCustomerIsRedirectedToLoginForCart() throws Exception {
        mockMvc.perform(get("/cart"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousCustomerCannotOpenCheckoutOrOrders() throws Exception {
        mockMvc.perform(get("/checkout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/my-orders"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/orders/1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void authenticatedUserCanRenderEmptyCartTemplate() throws Exception {
        mockMvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Giỏ hàng của bạn đang trống")));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void cartMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/cart/items").param("productId", "1").param("quantity", "1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCustomerCannotUseCartApi() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(csrf())
                .param("productId", "1").param("quantity", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void cartApiMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .param("productId", "1").param("quantity", "1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCustomerCannotPostReview() throws Exception {
        mockMvc.perform(post("/product/1/reviews").with(csrf())
                .param("rating", "5").param("comment", "Rất tốt"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void checkoutMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/checkout")
                .param("receiverName", "Nguyễn Văn An")
                .param("receiverPhone", "0901234567")
                .param("shippingAddress", "123 Nguyễn Trãi")
                .param("paymentMethod", "COD"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void normalUserCannotOpenAdminArea() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/products"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("ROLE_ADMIN")));

        mockMvc.perform(get("/admin/products"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý sản phẩm")));

        mockMvc.perform(get("/admin/orders"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý đơn hàng")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void adminMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/admin/products/1/toggle"))
            .andExpect(status().isForbidden());
    }

    @Test
    void registrationPostRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/register"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/register").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/register?databaseRequired"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void logoutUsesPostAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }
}
