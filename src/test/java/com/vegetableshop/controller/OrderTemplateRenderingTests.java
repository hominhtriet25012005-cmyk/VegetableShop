package com.vegetableshop.controller;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("template")
@Import(OrderTemplateRenderingTests.OrderPreviewController.class)
class OrderTemplateRenderingTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void checkoutRendersItemsValidationAndCsrfForm() throws Exception {
        mockMvc.perform(get("/__test/checkout-preview"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cà rốt Đà Lạt")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("50.000 ₫")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Thanh toán khi nhận hàng")));
    }

    @Test
    void historyAndDetailRenderSnapshotValues() throws Exception {
        mockMvc.perform(get("/__test/orders-preview"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("VS-TEST-001")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("50.000 ₫")));
        mockMvc.perform(get("/__test/order-detail-preview"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Nguyễn Văn An")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cà rốt Đà Lạt")));
    }

    @Controller
    static class OrderPreviewController {
        @GetMapping("/__test/checkout-preview")
        String checkout(Model model) {
            Product product = new Product();
            product.setName("Cà rốt Đà Lạt");
            product.setPrice(new BigDecimal("25000"));
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(2);
            model.addAttribute("checkoutRequest", new CheckoutRequest());
            model.addAttribute("cartItems", List.of(item));
            model.addAttribute("cartTotal", new BigDecimal("50000"));
            model.addAttribute("cartItemCount", 2);
            return "checkout";
        }

        @GetMapping("/__test/orders-preview")
        String orders(Model model) {
            model.addAttribute("orders", List.of(order()));
            model.addAttribute("cartItemCount", 0);
            return "my-orders";
        }

        @GetMapping("/__test/order-detail-preview")
        String detail(Model model) {
            model.addAttribute("order", order());
            model.addAttribute("cartItemCount", 0);
            return "order-detail";
        }

        private Order order() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderCode("VS-TEST-001");
            order.setReceiverName("Nguyễn Văn An");
            order.setReceiverPhone("0901234567");
            order.setShippingAddress("123 Nguyễn Trãi");
            order.setTotalAmount(new BigDecimal("50000"));
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(PaymentMethod.COD);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 8, 27, 10, 30));
            OrderDetail detail = new OrderDetail();
            detail.setProductName("Cà rốt Đà Lạt");
            detail.setPrice(new BigDecimal("25000"));
            detail.setQuantity(2);
            detail.setSubtotal(new BigDecimal("50000"));
            order.addDetail(detail);
            return order;
        }
    }
}
