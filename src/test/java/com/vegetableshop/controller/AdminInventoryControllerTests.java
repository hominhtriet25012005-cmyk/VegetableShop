package com.vegetableshop.controller;

import com.vegetableshop.dto.InventoryMovementRequest;
import com.vegetableshop.dto.InventoryOverviewView;
import com.vegetableshop.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInventoryControllerTests {

    @Mock private InventoryService inventoryService;

    @Test
    void overviewPassesFiltersAndExposesInventory() {
        InventoryOverviewView overview = new InventoryOverviewView(20, 1, 0, List.of());
        when(inventoryService.overview("nấm", "LOW")).thenReturn(overview);
        ConcurrentModel model = new ConcurrentModel();

        assertEquals("admin/inventory", controller().overview("nấm", "LOW", model));
        assertSame(overview, model.getAttribute("inventory"));
    }

    @Test
    void legacyMovementPostRedirectsToDocumentFormWithoutUpdatingStock() {
        InventoryMovementRequest request = new InventoryMovementRequest();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(request, "movementRequest");
        binding.rejectValue("reason", "blank", "Thiếu lý do");
        String view = controller().applyMovement(3L, request, binding, authentication(),
            new ConcurrentModel(), new RedirectAttributesModelMap());

        assertEquals("redirect:/admin/inventory/documents/new", view);
        verify(inventoryService, never()).applyManualMovement(3L, request, "admin@example.com");
    }

    @Test
    void legacyMovementGetRedirectsToMatchingDocumentType() {
        assertEquals("redirect:/admin/inventory/documents/new?type=OUTBOUND",
            controller().movementForm(3L, com.vegetableshop.entity.StockMovementType.OUTBOUND,
                new ConcurrentModel()));
        verify(inventoryService, never()).findProductRow(3L);
    }

    private AdminInventoryController controller() {
        return new AdminInventoryController(inventoryService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
            "admin@example.com", "ignored", List.of());
    }

}
