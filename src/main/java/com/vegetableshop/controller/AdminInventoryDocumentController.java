package com.vegetableshop.controller;

import com.vegetableshop.dto.InventoryDocumentRequest;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.InventoryDocumentService;
import jakarta.persistence.EntityNotFoundException;
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

@Controller
@Profile("mysql")
public class AdminInventoryDocumentController {

    private final InventoryDocumentService documentService;

    public AdminInventoryDocumentController(InventoryDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/admin/inventory/documents")
    public String documents(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) InventoryDocumentType type,
        @RequestParam(required = false) InventoryDocumentStatus status,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") int page,
        Model model
    ) {
        try {
            model.addAttribute("documentPage", documentService.documents(
                keyword, type, status, supplierId, productId, from, to, page));
        } catch (AdminOperationException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("documentPage", documentService.documents(
                "", null, null, null, productId, null, null, 0));
        }
        model.addAttribute("products", documentService.productsForForm());
        model.addAttribute("suppliers", documentService.suppliersForForm());
        model.addAttribute("documentTypes", InventoryDocumentType.values());
        model.addAttribute("documentStatuses", InventoryDocumentStatus.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedProduct", productId == null ? null : documentService.productForFilter(productId));
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        return "admin/inventory-documents";
    }

    /**
     * Compatibility helper for direct callers using the original argument list.
     */
    public String documents(
        String keyword,
        InventoryDocumentType type,
        InventoryDocumentStatus status,
        Long supplierId,
        LocalDate from,
        LocalDate to,
        int page,
        Model model
    ) {
        return documents(keyword, type, status, supplierId, null, from, to, page, model);
    }

    @GetMapping("/admin/inventory/documents/new")
    public String newDocument(
        @RequestParam(defaultValue = "INBOUND") InventoryDocumentType type,
        @RequestParam(required = false) Long productId,
        Model model
    ) {
        InventoryDocumentRequest request = new InventoryDocumentRequest();
        request.setType(type);
        // When an admin starts from a product row, keep that product selected in
        // the new document. Quantity and cost remain editable and must still be
        // confirmed by the admin before the stock is changed.
        if (productId != null && type != null) {
            var firstLine = request.getItems().getFirst();
            firstLine.setProductId(productId);
            firstLine.setQuantity(type == InventoryDocumentType.ADJUSTMENT ? 0 : 1);
        }
        model.addAttribute("documentRequest", request);
        addFormModel(model, null);
        return "admin/inventory-document-form";
    }

    /**
     * Backward-compatible helper for callers that open a blank document from
     * code (the HTTP route uses the overload above so it can accept productId).
     */
    public String newDocument(InventoryDocumentType type, Model model) {
        return newDocument(type, null, model);
    }

    @PostMapping("/admin/inventory/documents")
    public String createDocument(
        @Valid @ModelAttribute("documentRequest") InventoryDocumentRequest request,
        BindingResult result,
        Authentication authentication,
        Model model,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            addFormModel(model, null);
            return "admin/inventory-document-form";
        }
        try {
            var document = documentService.createDraft(request, authentication.getName());
            redirect.addFlashAttribute("successMessage", "Đã lưu phiếu kho ở trạng thái bản nháp");
            return "redirect:/admin/inventory/documents/" + document.getId();
        } catch (RuntimeException exception) {
            result.reject("document.failed", exception.getMessage());
            addFormModel(model, null);
            return "admin/inventory-document-form";
        }
    }

    @GetMapping("/admin/inventory/documents/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("document", documentService.findDetailed(id));
        return "admin/inventory-document-detail";
    }

    @GetMapping("/admin/inventory/documents/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("documentRequest", documentService.form(id));
        addFormModel(model, id);
        return "admin/inventory-document-form";
    }

    @PostMapping("/admin/inventory/documents/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("documentRequest") InventoryDocumentRequest request,
        BindingResult result,
        Model model,
        RedirectAttributes redirect
    ) {
        if (result.hasErrors()) {
            addFormModel(model, id);
            return "admin/inventory-document-form";
        }
        try {
            documentService.updateDraft(id, request);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật phiếu nháp");
            return "redirect:/admin/inventory/documents/" + id;
        } catch (RuntimeException exception) {
            result.reject("document.failed", exception.getMessage());
            addFormModel(model, id);
            return "admin/inventory-document-form";
        }
    }

    @PostMapping("/admin/inventory/documents/{id}/post")
    public String post(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        try {
            documentService.post(id, authentication.getName());
            redirect.addFlashAttribute("successMessage", "Đã xác nhận phiếu và cập nhật tồn kho");
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/inventory/documents/" + id;
    }

    @PostMapping("/admin/inventory/documents/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            documentService.deleteDraft(id);
            redirect.addFlashAttribute("successMessage", "Đã xóa phiếu nháp");
            return "redirect:/admin/inventory/documents";
        } catch (AdminOperationException | EntityNotFoundException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/inventory/documents/" + id;
        }
    }

    private void addFormModel(Model model, Long id) {
        model.addAttribute("documentId", id);
        model.addAttribute("products", documentService.productsForForm());
        model.addAttribute("suppliers", documentService.suppliersForForm());
        model.addAttribute("documentTypes", InventoryDocumentType.values());
    }
}
