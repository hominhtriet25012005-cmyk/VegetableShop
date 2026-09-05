package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminBrandRequest;
import com.vegetableshop.dto.AdminSupplierRequest;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.AdminCatalogService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
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
public class AdminCatalogController {

    private final AdminCatalogService catalogService;

    public AdminCatalogController(AdminCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/admin/brands")
    public String brands(@RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAttribute("brands", catalogService.brands(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/brands";
    }

    @GetMapping("/admin/brands/new")
    public String newBrand(Model model) {
        model.addAttribute("brandRequest", new AdminBrandRequest());
        model.addAttribute("brandId", null);
        return "admin/brand-form";
    }

    @PostMapping("/admin/brands")
    public String createBrand(@Valid @ModelAttribute("brandRequest") AdminBrandRequest request,
                              BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("brandId", null);
            return "admin/brand-form";
        }
        try {
            catalogService.createBrand(request);
            redirect.addFlashAttribute("successMessage", "Đã thêm thương hiệu");
            return "redirect:/admin/brands";
        } catch (AdminOperationException exception) {
            result.reject("brand.failed", exception.getMessage());
            model.addAttribute("brandId", null);
            return "admin/brand-form";
        }
    }

    @GetMapping("/admin/brands/{id}/edit")
    public String editBrand(@PathVariable Long id, Model model) {
        model.addAttribute("brandRequest", catalogService.brandForm(id));
        model.addAttribute("brandId", id);
        return "admin/brand-form";
    }

    @PostMapping("/admin/brands/{id}")
    public String updateBrand(@PathVariable Long id,
                              @Valid @ModelAttribute("brandRequest") AdminBrandRequest request,
                              BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("brandId", id);
            return "admin/brand-form";
        }
        try {
            catalogService.updateBrand(id, request);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật thương hiệu");
            return "redirect:/admin/brands";
        } catch (AdminOperationException | EntityNotFoundException exception) {
            result.reject("brand.failed", exception.getMessage());
            model.addAttribute("brandId", id);
            return "admin/brand-form";
        }
    }

    @PostMapping("/admin/brands/{id}/toggle")
    public String toggleBrand(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            boolean active = catalogService.toggleBrand(id);
            redirect.addFlashAttribute("successMessage", active ? "Đã kích hoạt thương hiệu" : "Đã ẩn thương hiệu");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/brands";
    }

    @GetMapping("/admin/suppliers")
    public String suppliers(@RequestParam(defaultValue = "") String keyword, Model model) {
        model.addAttribute("suppliers", catalogService.suppliers(keyword));
        model.addAttribute("keyword", keyword);
        return "admin/suppliers";
    }

    @GetMapping("/admin/suppliers/new")
    public String newSupplier(Model model) {
        model.addAttribute("supplierRequest", new AdminSupplierRequest());
        model.addAttribute("supplierId", null);
        return "admin/supplier-form";
    }

    @PostMapping("/admin/suppliers")
    public String createSupplier(@Valid @ModelAttribute("supplierRequest") AdminSupplierRequest request,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("supplierId", null);
            return "admin/supplier-form";
        }
        try {
            catalogService.createSupplier(request);
            redirect.addFlashAttribute("successMessage", "Đã thêm nhà cung cấp");
            return "redirect:/admin/suppliers";
        } catch (AdminOperationException exception) {
            result.reject("supplier.failed", exception.getMessage());
            model.addAttribute("supplierId", null);
            return "admin/supplier-form";
        }
    }

    @GetMapping("/admin/suppliers/{id}/edit")
    public String editSupplier(@PathVariable Long id, Model model) {
        model.addAttribute("supplierRequest", catalogService.supplierForm(id));
        model.addAttribute("supplierId", id);
        return "admin/supplier-form";
    }

    @PostMapping("/admin/suppliers/{id}")
    public String updateSupplier(@PathVariable Long id,
                                 @Valid @ModelAttribute("supplierRequest") AdminSupplierRequest request,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("supplierId", id);
            return "admin/supplier-form";
        }
        try {
            catalogService.updateSupplier(id, request);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật nhà cung cấp");
            return "redirect:/admin/suppliers";
        } catch (AdminOperationException | EntityNotFoundException exception) {
            result.reject("supplier.failed", exception.getMessage());
            model.addAttribute("supplierId", id);
            return "admin/supplier-form";
        }
    }

    @PostMapping("/admin/suppliers/{id}/toggle")
    public String toggleSupplier(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            boolean active = catalogService.toggleSupplier(id);
            redirect.addFlashAttribute("successMessage", active ? "Đã kích hoạt nhà cung cấp" : "Đã ngừng hợp tác");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/suppliers";
    }
}
