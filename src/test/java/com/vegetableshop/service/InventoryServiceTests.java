package com.vegetableshop.service;

import com.vegetableshop.dto.InventoryMovementRequest;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.StockMovement;
import com.vegetableshop.entity.StockMovementType;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository stockMovementRepository;

    @Test
    void inboundUpdatesLockedProductAndWritesCompleteLedgerEntry() {
        Product product = product(1L, "Cà rốt", 10, 4);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement result = service().applyManualMovement(
            1L, request(StockMovementType.INBOUND, 7, "Nhập từ nhà cung cấp"), "admin@example.com");

        assertEquals(17, product.getQuantity());
        assertEquals(10, result.getQuantityBefore());
        assertEquals(7, result.getQuantityChange());
        assertEquals(17, result.getQuantityAfter());
        assertEquals("admin@example.com", result.getPerformedBy());
    }

    @Test
    void outboundCannotMakeStockNegativeAndDoesNotWriteHistory() {
        Product product = product(2L, "Cam", 3, 2);
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(product));

        assertThrows(AdminOperationException.class, () -> service().applyManualMovement(
            2L, request(StockMovementType.OUTBOUND, 4, "Hàng hỏng"), "admin@example.com"));

        assertEquals(3, product.getQuantity());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void adjustmentTreatsQuantityAsPhysicalCount() {
        Product product = product(3L, "Nấm", 12, 5);
        when(productRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        StockMovement result = service().applyManualMovement(
            3L, request(StockMovementType.ADJUSTMENT, 8, "Kiểm kê cuối tháng"), "admin@example.com");

        assertEquals(8, product.getQuantity());
        assertEquals(-4, result.getQuantityChange());
        assertEquals(StockMovementType.ADJUSTMENT, result.getMovementType());
    }

    @Test
    void overviewUsesPerProductThresholdAndKeepsGlobalMetricsWhenFiltering() {
        Product low = product(1L, "Nấm hương", 4, 5);
        Product out = product(2L, "Cam", 0, 10);
        Product healthy = product(3L, "Cà rốt", 20, 10);
        when(productRepository.findAllByOrderByQuantityAscNameAsc())
            .thenReturn(List.of(out, low, healthy));

        var overview = service().overview("nấm", "LOW");

        assertEquals(24, overview.totalInventoryUnits());
        assertEquals(1, overview.lowStockProductCount());
        assertEquals(1, overview.outOfStockProductCount());
        assertEquals(List.of("Nấm hương"), overview.products().stream().map(row -> row.productName()).toList());
    }

    @Test
    void cancelledOrderRestoresStockAndRecordsOrderReference() {
        Product product = product(5L, "Khoai tây", 6, 3);
        Order order = new Order();
        order.setOrderCode("VS-001");
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(2);
        order.addDetail(detail);
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service().restoreCancelledOrder(order, "admin@example.com");

        assertEquals(8, product.getQuantity());
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertEquals(StockMovementType.RETURN, captor.getValue().getMovementType());
        assertEquals("VS-001", captor.getValue().getReferenceId());
    }

    @Test
    void thresholdValidationRejectsNegativeValue() {
        assertThrows(AdminOperationException.class, () -> service().updateLowStockThreshold(1L, -1));
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void historyRejectsReversedDateRange() {
        assertThrows(AdminOperationException.class, () -> service().history(
            null, null, LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 1), 0));
        verify(stockMovementRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.Pageable.class));
    }

    private InventoryService service() {
        return new InventoryService(productRepository, stockMovementRepository);
    }

    private InventoryMovementRequest request(StockMovementType type, int quantity, String reason) {
        InventoryMovementRequest request = new InventoryMovementRequest();
        request.setMovementType(type);
        request.setQuantity(quantity);
        request.setReason(reason);
        return request;
    }

    private Product product(Long id, String name, int quantity, int threshold) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setQuantity(quantity);
        product.setLowStockThreshold(threshold);
        product.setCategory(category);
        product.setStatus(true);
        return product;
    }
}
