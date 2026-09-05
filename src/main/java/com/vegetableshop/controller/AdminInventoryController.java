package com.vegetableshop.controller;

import com.vegetableshop.dto.InventoryMovementRequest;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.Arrays;

@Controller
@Profile("mysql")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/admin/inventory")
    public String overview(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "ALL") String stockStatus,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        model.addAttribute("inventory", inventoryService.overview(keyword, stockStatus, page));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStockStatus", stockStatus);
        return "admin/inventory";
    }

    public String overview(String keyword, String stockStatus, Model model) {
        model.addAttribute("inventory", inventoryService.overview(keyword, stockStatus));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStockStatus", stockStatus);
        return "admin/inventory";
    }

    @GetMapping("/admin/inventory/{productId}/movement")
    public String movementForm(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "INBOUND") StockMovementType type,
        Model model
    ) {
        StockMovementType safeType = type != null && type.isManual() ? type : StockMovementType.INBOUND;
        return "redirect:/admin/inventory/documents/new?type=" + safeType.name();
    }

    @PostMapping("/admin/inventory/{productId}/movement")
    public String applyMovement(
        @PathVariable Long productId,
        @Valid @ModelAttribute("movementRequest") InventoryMovementRequest request,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes attributes
    ) {
        attributes.addFlashAttribute("errorMessage",
            "Cập nhật kho trực tiếp đã ngừng sử dụng. Vui lòng lập phiếu kho.");
        return "redirect:/admin/inventory/documents/new";
    }

    @PostMapping("/admin/inventory/{productId}/threshold")
    public String updateThreshold(
        @PathVariable Long productId,
        @RequestParam int threshold,
        RedirectAttributes attributes
    ) {
        try {
            inventoryService.updateLowStockThreshold(productId, threshold);
            attributes.addFlashAttribute("successMessage", "Đã cập nhật ngưỡng cảnh báo tồn kho");
        } catch (RuntimeException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    @GetMapping("/admin/inventory/history")
    public String history(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) StockMovementType movementType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        try {
            model.addAttribute("movementPage",
                inventoryService.history(productId, movementType, from, to, page));
        } catch (AdminOperationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("movementPage", inventoryService.history(null, null, null, null, 0));
        }
        model.addAttribute("products", inventoryService.productsForFilter());
        model.addAttribute("movementTypes", StockMovementType.values());
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedMovementType", movementType);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "admin/inventory-history";
    }

    private void addMovementModel(Model model, Long productId) {
        model.addAttribute("product", inventoryService.findProductRow(productId));
        model.addAttribute("manualMovementTypes", Arrays.stream(StockMovementType.values())
            .filter(StockMovementType::isManual)
            .toList());
    }
}
