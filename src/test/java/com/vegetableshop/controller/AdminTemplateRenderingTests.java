package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(AdminTemplateRenderingTests.AdminPreviewController.class)
class AdminTemplateRenderingTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void dashboardRendersDatabaseMetricsRecentOrderAndLowStock() throws Exception {
        mockMvc.perform(get("/__test/admin-dashboard").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("250.000 ₫")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("VS-ADMIN-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cà rốt sắp hết")));
    }

    @Test
    void productAndUserTablesRenderDataWithoutPassword() throws Exception {
        mockMvc.perform(get("/__test/admin-products").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cam kiểm thử")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tất cả danh mục")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tất cả trạng thái")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Quay về website")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Giai đoạn 8"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Md. Hasan Mahmud"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/__test/admin-users").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("user@example.com")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bcrypt-secret"))));
    }

    @Test
    void productFormContainsBackendValidationAndCsrf() throws Exception {
        mockMvc.perform(get("/__test/admin-product-form").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lưu sản phẩm")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    @Test
    void categoriesAndOrderDetailRenderAdminActions() throws Exception {
        mockMvc.perform(get("/__test/admin-categories").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Rau củ")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/__test/admin-order-detail").with(user("admin@example.com").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("VS-ADMIN-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("CONFIRMED")));
    }

    @Controller
    static class AdminPreviewController {

        @GetMapping("/__test/admin-dashboard")
        String dashboard(Model model) {
            Product lowStock = product(3L, "Cà rốt sắp hết", 4);
            Order order = order();
            model.addAttribute("dashboard", new AdminDashboardView(
                5, 3, 2, 1, new BigDecimal("250000"), List.of(order), List.of(lowStock)
            ));
            return "admin/dashboard";
        }

        @GetMapping("/__test/admin-products")
        String products(Model model) {
            Product product = product(4L, "Cam kiểm thử", 12);
            model.addAttribute("productPage", new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));
            model.addAttribute("keyword", "");
            model.addAttribute("categories", List.of(category()));
            model.addAttribute("selectedCategoryId", 1L);
            model.addAttribute("selectedStatus", true);
            return "admin/products";
        }

        @GetMapping("/__test/admin-users")
        String users(Model model) {
            User user = new User();
            user.setId(7L);
            user.setFullName("Người dùng kiểm thử");
            user.setEmail("user@example.com");
            user.setPassword("bcrypt-secret");
            user.setRole(Role.USER);
            user.setStatus(true);
            model.addAttribute("userPage", new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));
            model.addAttribute("keyword", "");
            return "admin/users";
        }

        @GetMapping("/__test/admin-product-form")
        String productForm(Model model) {
            Category category = category();
            AdminProductRequest request = new AdminProductRequest();
            request.setCategoryId(category.getId());
            model.addAttribute("productRequest", request);
            model.addAttribute("categories", List.of(category));
            model.addAttribute("productId", null);
            return "admin/product-form";
        }

        @GetMapping("/__test/admin-categories")
        String categories(Model model) {
            model.addAttribute("categories", List.of(category()));
            return "admin/categories";
        }

        @GetMapping("/__test/admin-order-detail")
        String orderDetail(Model model) {
            model.addAttribute("order", order());
            model.addAttribute("allowedStatuses", List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
            return "admin/order-detail";
        }

        private static Product product(Long id, String name, int stock) {
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setPrice(new BigDecimal("35000"));
            product.setQuantity(stock);
            product.setImage("/img/vegetable-item-1.jpg");
            product.setStatus(true);
            product.setCategory(category());
            return product;
        }

        private static Category category() {
            Category category = new Category();
            category.setId(1L);
            category.setName("Rau củ");
            category.setStatus(true);
            return category;
        }

        private static Order order() {
            User user = new User();
            user.setFullName("Khách kiểm thử");
            Order order = new Order();
            order.setId(1L);
            order.setOrderCode("VS-ADMIN-001");
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(PaymentMethod.COD);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setReceiverName("Khách kiểm thử");
            order.setReceiverPhone("0901234567");
            order.setShippingAddress("123 Nguyễn Trãi");
            order.setTotalAmount(new BigDecimal("250000"));
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 8, 27, 20, 0));
            return order;
        }
    }
}
