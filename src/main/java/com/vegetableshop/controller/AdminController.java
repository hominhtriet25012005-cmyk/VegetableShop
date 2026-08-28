package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", adminService.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/admin/products")
    public String products(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Boolean status,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("productPage", adminService.findProducts(keyword, categoryId, status, page));
        model.addAttribute("categories", adminService.findCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStatus", status);
        return "admin/products";
    }

    @GetMapping("/admin/products/new")
    public String newProduct(Model model) {
        model.addAttribute("productRequest", new AdminProductRequest());
        addProductFormModel(model, null);
        return "admin/product-form";
    }

    @PostMapping("/admin/products")
    public String createProduct(
        @Valid @ModelAttribute("productRequest") AdminProductRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addProductFormModel(model, null);
            return "admin/product-form";
        }
        try {
            adminService.createProduct(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm");
            return "redirect:/admin/products";
        } catch (AdminOperationException | EntityNotFoundException | ProductNotFoundException exception) {
            bindingResult.reject("product.failed", exception.getMessage());
            addProductFormModel(model, null);
            return "admin/product-form";
        }
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        model.addAttribute("productRequest", adminService.getProductForm(id));
        addProductFormModel(model, id);
        return "admin/product-form";
    }

    @PostMapping("/admin/products/{id}")
    public String updateProduct(
        @PathVariable Long id,
        @Valid @ModelAttribute("productRequest") AdminProductRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addProductFormModel(model, id);
            return "admin/product-form";
        }
        try {
            adminService.updateProduct(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm");
            return "redirect:/admin/products";
        } catch (AdminOperationException | EntityNotFoundException | ProductNotFoundException exception) {
            bindingResult.reject("product.failed", exception.getMessage());
            addProductFormModel(model, id);
            return "admin/product-form";
        }
    }

    @PostMapping("/admin/products/{id}/toggle")
    public String toggleProduct(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            boolean active = adminService.toggleProductStatus(id);
            attributes.addFlashAttribute("successMessage", active ? "Đã kích hoạt sản phẩm" : "Đã ẩn sản phẩm");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/admin/categories")
    public String categories(Model model) {
        model.addAttribute("categories", adminService.findCategories());
        return "admin/categories";
    }

    @GetMapping("/admin/categories/new")
    public String newCategory(Model model) {
        model.addAttribute("categoryRequest", new AdminCategoryRequest());
        addCategoryFormModel(model, null);
        return "admin/category-form";
    }

    @PostMapping("/admin/categories")
    public String createCategory(
        @Valid @ModelAttribute("categoryRequest") AdminCategoryRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes attributes
    ) {
        if (bindingResult.hasErrors()) {
            addCategoryFormModel(model, null);
            return "admin/category-form";
        }
        try {
            adminService.createCategory(request);
            attributes.addFlashAttribute("successMessage", "Đã thêm danh mục");
            return "redirect:/admin/categories";
        } catch (AdminOperationException exception) {
            bindingResult.reject("category.failed", exception.getMessage());
            addCategoryFormModel(model, null);
            return "admin/category-form";
        }
    }

    @GetMapping("/admin/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model model) {
        model.addAttribute("categoryRequest", adminService.getCategoryForm(id));
        addCategoryFormModel(model, id);
        return "admin/category-form";
    }

    @PostMapping("/admin/categories/{id}")
    public String updateCategory(
        @PathVariable Long id,
        @Valid @ModelAttribute("categoryRequest") AdminCategoryRequest request,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes attributes
    ) {
        if (bindingResult.hasErrors()) {
            addCategoryFormModel(model, id);
            return "admin/category-form";
        }
        try {
            adminService.updateCategory(id, request);
            attributes.addFlashAttribute("successMessage", "Đã cập nhật danh mục");
            return "redirect:/admin/categories";
        } catch (AdminOperationException exception) {
            bindingResult.reject("category.failed", exception.getMessage());
            addCategoryFormModel(model, id);
            return "admin/category-form";
        }
    }

    @PostMapping("/admin/categories/{id}/toggle")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            boolean active = adminService.toggleCategoryStatus(id);
            attributes.addFlashAttribute("successMessage", active ? "Đã kích hoạt danh mục" : "Đã ẩn danh mục");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/orders")
    public String orders(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("orderPage", adminService.findOrders(keyword, status, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "admin/orders";
    }

    @GetMapping("/admin/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        var order = adminService.findOrder(id);
        model.addAttribute("order", order);
        model.addAttribute("allowedStatuses", adminService.allowedTransitions(order.getStatus()));
        return "admin/order-detail";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateOrderStatus(
        @PathVariable Long id,
        @RequestParam OrderStatus status,
        RedirectAttributes attributes
    ) {
        try {
            adminService.updateOrderStatus(id, status);
            attributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @GetMapping("/admin/users")
    public String users(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("userPage", adminService.findUsers(keyword, page));
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUser(
        Authentication authentication,
        @PathVariable Long id,
        RedirectAttributes attributes
    ) {
        try {
            boolean active = adminService.toggleUserStatus(id, authentication.getName());
            attributes.addFlashAttribute("successMessage", active ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void addProductFormModel(Model model, Long id) {
        model.addAttribute("categories", adminService.findCategories());
        model.addAttribute("suppliers", adminService.findSuppliers());
        model.addAttribute("productId", id);
    }

    private void addCategoryFormModel(Model model, Long id) {
        model.addAttribute("categoryId", id);
    }
}
