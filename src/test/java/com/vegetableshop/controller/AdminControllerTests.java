package com.vegetableshop.controller;

import com.vegetableshop.dto.AdminDashboardView;
import com.vegetableshop.dto.AdminProductRequest;
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
        when(adminService.findProducts("cam", 3L, false, 2)).thenReturn(page);
        when(adminService.findCategories()).thenReturn(List.of(category));
        ConcurrentModel model = new ConcurrentModel();

        assertEquals("admin/products", controller().products("cam", 3L, false, 2, model));
        assertSame(page, model.getAttribute("productPage"));
        assertEquals(List.of(category), model.getAttribute("categories"));
        assertEquals(3L, model.getAttribute("selectedCategoryId"));
        assertEquals(false, model.getAttribute("selectedStatus"));
    }

    @Test
    void invalidProductFormDoesNotWriteDatabase() {
        AdminProductRequest request = new AdminProductRequest();
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(request, "productRequest");
        result.rejectValue("name", "blank", "Tên trống");

        assertEquals("admin/product-form",
            controller().createProduct(request, result, new ConcurrentModel(), new RedirectAttributesModelMap()));
        verify(adminService, never()).createProduct(request);
    }

    @Test
    void illegalOrderTransitionReturnsFriendlyFlashError() {
        when(adminService.updateOrderStatus(7L, OrderStatus.COMPLETED))
            .thenThrow(new AdminOperationException("Không thể chuyển trạng thái"));
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller().updateOrderStatus(7L, OrderStatus.COMPLETED, attributes);

        assertEquals("redirect:/admin/orders/7", view);
        assertEquals("Không thể chuyển trạng thái", attributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void userToggleAlwaysUsesAuthenticatedAdminEmail() {
        controller().toggleUser(authentication(), 8L, new RedirectAttributesModelMap());
        verify(adminService).toggleUserStatus(8L, "admin@example.com");
    }

    private AdminController controller() {
        return new AdminController(adminService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated("admin@example.com", "ignored", List.of());
    }
}
