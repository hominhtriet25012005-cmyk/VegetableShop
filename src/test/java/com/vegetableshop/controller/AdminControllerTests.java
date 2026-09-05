package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.dto.AdminReportView;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTests {

    @Mock private AdminService adminService;

    @Test
    void dashboardAddsDatabaseSummaryToModel() {
        AdminDashboardView dashboard = new AdminDashboardView(5, 3, 2, 1,
            BigDecimal.TEN, List.of(), List.of());
        when(adminService.dashboard()).thenReturn(dashboard);
        ConcurrentModel model = new ConcurrentModel();

        assertEquals("admin/dashboard", controller().dashboard(model));
        assertSame(dashboard, model.getAttribute("dashboard"));
    }

    @Test
    void productListPassesSearchAndPageToService() {
        var page = new PageImpl<Product>(List.of());
        Category category = new Category();
        category.setId(3L);
        when(adminService.findProducts("cam", 3L, 4L, false, 2)).thenReturn(page);
        when(adminService.findCategories()).thenReturn(List.of(category));
        ConcurrentModel model = new ConcurrentModel();

        assertEquals("admin/products", controller().products("cam", 3L, 4L, false, 2, model));
        assertSame(page, model.getAttribute("productPage"));
        assertEquals(List.of(category), model.getAttribute("categories"));
        assertEquals(3L, model.getAttribute("selectedCategoryId"));
        assertEquals(4L, model.getAttribute("selectedBrandId"));
        assertEquals(false, model.getAttribute("selectedStatus"));
    }

    @Test
    void invalidProductFormDoesNotWriteDatabase() {
        AdminProductRequest request = new AdminProductRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "productRequest");
        result.rejectValue("name", "blank", "Tên trống");

        assertEquals("admin/product-form",
            controller().createProduct(request, result, new ConcurrentModel(), authentication(),
                new RedirectAttributesModelMap()));
        verify(adminService, never()).createProduct(request, "admin@example.com");
    }

    @Test
    void productUpdateReturnsToOriginalPageFiltersAndProductRow() {
        AdminProductRequest request = new AdminProductRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "productRequest");

        String view = controller().updateProduct(
            24L, request, result, new ConcurrentModel(), authentication(),
            new RedirectAttributesModelMap(), "hat-bi", 2L, 3L, true, 1);

        assertEquals(
            "redirect:/admin/products?keyword=hat-bi&categoryId=2&brandId=3&status=true&page=1#product-24",
            view);
        verify(adminService).updateProduct(24L, request, "admin@example.com");
    }

    @Test
    void illegalOrderTransitionReturnsFriendlyFlashError() {
        when(adminService.updateOrderStatus(7L, OrderStatus.COMPLETED, "admin@example.com"))
            .thenThrow(new AdminOperationException("Không thể chuyển trạng thái"));
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller().updateOrderStatus(authentication(), 7L, OrderStatus.COMPLETED, attributes);

        assertEquals("redirect:/admin/orders/7", view);
        assertEquals("Không thể chuyển trạng thái", attributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void userToggleAlwaysUsesAuthenticatedAdminEmail() {
        controller().toggleUser(authentication(), 8L, new RedirectAttributesModelMap());
        verify(adminService).toggleUserStatus(8L, "admin@example.com");
    }

    @Test
    void reportsPassDateRangeToServiceAndExposeView() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        AdminReportView report = emptyReport(from, to);
        when(adminService.businessReport(from, to)).thenReturn(report);
        ConcurrentModel model = new ConcurrentModel();

        assertEquals("admin/reports", controller().reports(from, to, model));
        assertSame(report, model.getAttribute("report"));
    }

    @Test
    void reportCsvUsesDownloadFilenameAndServiceBytes() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        AdminReportView report = emptyReport(from, to);
        byte[] csv = "csv".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(adminService.businessReport(from, to)).thenReturn(report);
        when(adminService.exportBusinessReportCsv(report)).thenReturn(csv);

        var response = controller().exportReports(from, to);

        assertSame(csv, response.getBody());
        assertEquals("attachment; filename=\"bao-cao-kinh-doanh-2026-01-01-den-2026-01-31.csv\"",
            response.getHeaders().getFirst("Content-Disposition"));
    }

    private AdminController controller() {
        return new AdminController(adminService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated("admin@example.com", "ignored", List.of());
    }

    private AdminReportView emptyReport(LocalDate from, LocalDate to) {
        return new AdminReportView(from, to, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO,
            0, 0, 0, 10, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of());
    }
}
