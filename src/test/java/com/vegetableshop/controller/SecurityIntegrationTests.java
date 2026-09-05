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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
class SecurityIntegrationTests {

    @Test @WithMockUser(roles = "USER")
    void customerCannotReadOrModerateAdminReviews() throws Exception {
        mockMvc.perform(get("/admin/reviews")).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/reviews/1/moderate").with(csrf()).param("status", "APPROVED"))
            .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void moderationAndReviewSubmissionRequireCsrf() throws Exception {
        mockMvc.perform(post("/admin/reviews/1/moderate").param("status", "DELETED"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/product/1/reviews").param("rating", "5").param("orderDetailId", "1"))
            .andExpect(status().isForbidden());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đăng nhập")))
            .andExpect(content().string(containsString(">Trang chủ</a>")))
            .andExpect(content().string(containsString(">Tin tức</a>")))
            .andExpect(content().string(containsString("Về Vegetable Shop")))
            .andExpect(content().string(not(containsString("Quay lại cửa hàng"))))
            .andExpect(content().string(not(containsString("Đăng nhập với Google"))))
            .andExpect(content().string(not(containsString("Facebook"))));

        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo tài khoản khách hàng")))
            .andExpect(content().string(containsString(">Trang chủ</a>")))
            .andExpect(content().string(containsString(">Tin tức</a>")))
            .andExpect(content().string(containsString("Về Vegetable Shop")))
            .andExpect(content().string(containsString("name=\"captchaAnswer\"")))
            .andExpect(content().string(containsString("3 + 4 = ?")));

        mockMvc.perform(get("/news"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tin tức")));

        mockMvc.perform(get("/api/chatbot/messages").param("message", "Tìm sản phẩm"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("chế độ xem giao diện")));
    }

    @Test
    void passwordRecoveryPagesArePublicButAccountPageIsProtected() throws Exception {
        mockMvc.perform(get("/forgot-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Quên mật khẩu")))
            .andExpect(content().string(containsString("name=\"captchaAnswer\"")))
            .andExpect(content().string(containsString("5 + 2 = ?")));

        mockMvc.perform(get("/contact"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kết nối với chúng tôi")))
            .andExpect(content().string(containsString("Ho+Chi+Minh+City")))
            .andExpect(content().string(not(containsString("New York"))));

        mockMvc.perform(get("/reset-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Liên kết không hợp lệ")));

        mockMvc.perform(get("/account"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void forgotPasswordPostRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "user@example.com"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", "user@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password?databaseRequired"));
    }

    @Test
    void accountActivationPagesArePublicAndResendRequiresCsrf() throws Exception {
        mockMvc.perform(get("/activate-account"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kích hoạt tài khoản")));

        mockMvc.perform(get("/resend-activation"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Gửi lại email kích hoạt")));

        mockMvc.perform(post("/resend-activation").param("email", "user@example.com"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/resend-activation").with(csrf()).param("email", "user@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/resend-activation?databaseRequired"));
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
    void anonymousCustomerCannotOpenOrMutateWishlist() throws Exception {
        mockMvc.perform(get("/wishlist"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/api/wishlist/items").with(csrf()).param("productId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void inventoryAdministrationRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/inventory"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/inventory").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                    .user("user@example.com").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void wishlistMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/wishlist/items").param("productId", "1"))
            .andExpect(status().isForbidden());
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
