package com.vegetableshop.controller;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.dto.ForgotPasswordRequest;
import com.vegetableshop.dto.ResetPasswordRequest;
import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.math.BigDecimal;

/**
 * Allows the converted Thymeleaf template to be checked without database
 * credentials. The real /shop controller is ProductController in mysql mode.
 */
@Controller
@Profile("template")
public class TemplateModeController {

    @GetMapping("/shop")
    public String shop(@ModelAttribute("filter") ProductFilter filter, Model model) {
        filter.normalize();
        Page<Product> productPage = Page.empty(PageRequest.of(filter.getPage(), filter.getSize()));
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", List.of());
        model.addAttribute("featuredProducts", List.of());
        model.addAttribute("categories", List.of());
        return "shop";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("googleLoginEnabled", false);
        model.addAttribute("mailEnabled", false);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(
        @ModelAttribute("registerRequest") RegisterRequest registerRequest,
        Model model
    ) {
        model.addAttribute("captchaQuestion", "3 + 4 = ?");
        return "register";
    }

    @PostMapping("/register")
    public String registerWithoutDatabase() {
        return "redirect:/register?databaseRequired";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(
        @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest forgotPasswordRequest,
        Model model
    ) {
        model.addAttribute("mailEnabled", false);
        model.addAttribute("captchaQuestion", "5 + 2 = ?");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordWithoutDatabase() {
        return "redirect:/forgot-password?databaseRequired";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(
        @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
        Model model
    ) {
        model.addAttribute("invalidToken", true);
        return "reset-password";
    }

    @GetMapping("/activate-account")
    public String activationResult(Model model) {
        model.addAttribute("activationSuccess", false);
        model.addAttribute("activationError", "Chức năng này cần chạy với profile mysql,mail");
        return "activation-result";
    }

    @GetMapping("/resend-activation")
    public String resendActivationForm(
        @ModelAttribute("activationRequest") ForgotPasswordRequest activationRequest,
        Model model
    ) {
        model.addAttribute("mailEnabled", false);
        return "resend-activation";
    }

    @PostMapping("/resend-activation")
    public String resendActivationWithoutDatabase() {
        return "redirect:/resend-activation?databaseRequired";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("cartItems", List.of());
        model.addAttribute("cartTotal", BigDecimal.ZERO);
        model.addAttribute("cartItemCount", 0);
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout(@ModelAttribute("checkoutRequest") CheckoutRequest checkoutRequest, Model model) {
        model.addAttribute("cartItems", List.of());
        model.addAttribute("cartTotal", BigDecimal.ZERO);
        return "checkout";
    }

    @GetMapping("/my-orders")
    public String myOrders(Model model) {
        model.addAttribute("orders", List.of());
        return "my-orders";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("dashboard", new AdminDashboardView(
            0, 0, 0, 0, BigDecimal.ZERO, List.of(), List.of()
        ));
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Model model) {
        Page<Product> page = Page.empty(PageRequest.of(0, 10));
        model.addAttribute("productPage", page);
        model.addAttribute("keyword", "");
        return "admin/products";
    }

    @GetMapping("/admin/products/new")
    public String adminProductForm(Model model) {
        model.addAttribute("productRequest", new AdminProductRequest());
        model.addAttribute("categories", List.<Category>of());
        model.addAttribute("productId", null);
        return "admin/product-form";
    }

    @GetMapping("/admin/categories")
    public String adminCategories(Model model) {
        model.addAttribute("categories", List.<Category>of());
        return "admin/categories";
    }

    @GetMapping("/admin/categories/new")
    public String adminCategoryForm(Model model) {
        model.addAttribute("categoryRequest", new AdminCategoryRequest());
        model.addAttribute("categoryId", null);
        return "admin/category-form";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        Page<Order> page = Page.empty(PageRequest.of(0, 10));
        model.addAttribute("orderPage", page);
        model.addAttribute("keyword", "");
        model.addAttribute("selectedStatus", null);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        Page<User> page = Page.empty(PageRequest.of(0, 10));
        model.addAttribute("userPage", page);
        model.addAttribute("keyword", "");
        return "admin/users";
    }
}
