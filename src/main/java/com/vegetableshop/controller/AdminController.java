package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.service.AdminService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

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

    @GetMapping("/admin/reports")
    public String reports(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Model model
    ) {
        try {
            model.addAttribute("report", adminService.businessReport(from, to));
        } catch (AdminOperationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("report", adminService.businessReport(null, null));
        }
        return "admin/reports";
    }

    @GetMapping("/admin/reports/export.csv")
    public ResponseEntity<byte[]> exportReports(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var report = adminService.businessReport(from, to);
        String filename = "bao-cao-kinh-doanh-" + report.fromDate() + "-den-" + report.toDate() + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(adminService.exportBusinessReportCsv(report));
    }

    @GetMapping("/admin/products")
    public String products(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) Boolean status,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("productPage", adminService.findProducts(keyword, categoryId, brandId, status, page));
        model.addAttribute("categories", adminService.findCategories());
        model.addAttribute("brands", adminService.findBrands());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedBrandId", brandId);
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
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addProductFormModel(model, null);
            return "admin/product-form";
        }
        try {
            adminService.createProduct(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm");
            return "redirect:/admin/products";
        } catch (AdminOperationException | EntityNotFoundException | ProductNotFoundException exception) {
            bindingResult.reject("product.failed", exception.getMessage());
            addProductFormModel(model, null);
            return "admin/product-form";
        }
    }

    @GetMapping("/admin/products/{id}/edit")
    public String editProduct(
        @PathVariable Long id,
        @RequestParam(defaultValue = "") String returnKeyword,
        @RequestParam(required = false) Long returnCategoryId,
        @RequestParam(required = false) Long returnBrandId,
        @RequestParam(required = false) Boolean returnStatus,
        @RequestParam(defaultValue = "0") int returnPage,
        Model model
    ) {
        model.addAttribute("productRequest", adminService.getProductForm(id));
        addProductFormModel(model, id);
        addProductReturnState(model, returnKeyword, returnCategoryId, returnBrandId, returnStatus, returnPage);
        return "admin/product-form";
    }

    public String editProduct(Long id, Model model) {
        return editProduct(id, "", null, null, null, 0, model);
    }

    @PostMapping("/admin/products/{id}")
    public String updateProduct(
        @PathVariable Long id,
        @Valid @ModelAttribute("productRequest") AdminProductRequest request,
        BindingResult bindingResult,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes,
        @RequestParam(defaultValue = "") String returnKeyword,
        @RequestParam(required = false) Long returnCategoryId,
        @RequestParam(required = false) Long returnBrandId,
        @RequestParam(required = false) Boolean returnStatus,
        @RequestParam(defaultValue = "0") int returnPage
    ) {
        if (bindingResult.hasErrors()) {
            addProductFormModel(model, id);
            addProductReturnState(model, returnKeyword, returnCategoryId, returnBrandId, returnStatus, returnPage);
            return "admin/product-form";
        }
        try {
            adminService.updateProduct(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật sản phẩm");
            return redirectToProducts(returnKeyword, returnCategoryId, returnBrandId, returnStatus, returnPage, id);
        } catch (AdminOperationException | EntityNotFoundException | ProductNotFoundException exception) {
            bindingResult.reject("product.failed", exception.getMessage());
            addProductFormModel(model, id);
            addProductReturnState(model, returnKeyword, returnCategoryId, returnBrandId, returnStatus, returnPage);
            return "admin/product-form";
        }
    }

    public String updateProduct(
        Long id,
        AdminProductRequest request,
        BindingResult bindingResult,
        Model model,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        return updateProduct(id, request, bindingResult, model, authentication, redirectAttributes,
            "", null, null, null, 0);
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
        Authentication authentication,
        @PathVariable Long id,
        @RequestParam OrderStatus status,
        RedirectAttributes attributes
    ) {
        try {
            adminService.updateOrderStatus(id, status, authentication.getName());
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
        model.addAttribute("brands", adminService.findBrands());
        model.addAttribute("productUnits", ProductUnit.values());
        model.addAttribute("productId", id);
    }

    private void addProductReturnState(
        Model model,
        String keyword,
        Long categoryId,
        Long brandId,
        Boolean status,
        int page
    ) {
        model.addAttribute("returnKeyword", keyword == null ? "" : keyword);
        model.addAttribute("returnCategoryId", categoryId);
        model.addAttribute("returnBrandId", brandId);
        model.addAttribute("returnStatus", status);
        model.addAttribute("returnPage", Math.max(page, 0));
    }

    private String redirectToProducts(
        String keyword,
        Long categoryId,
        Long brandId,
        Boolean status,
        int page,
        Long productId
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/products");
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        if (categoryId != null) builder.queryParam("categoryId", categoryId);
        if (brandId != null) builder.queryParam("brandId", brandId);
        if (status != null) builder.queryParam("status", status);
        if (page > 0) builder.queryParam("page", page);
        if (productId != null) builder.fragment("product-" + productId);
        return "redirect:" + builder.build().encode().toUriString();
    }

    private void addCategoryFormModel(Model model, Long id) {
        model.addAttribute("categoryId", id);
    }
}
