package com.vegetableshop.service;

import com.vegetableshop.dto.AdminReportView;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Supplier;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.CategoryRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.SupplierRepository;
import com.vegetableshop.repository.BrandRepository;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBusinessReportTests {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private InventoryDocumentService inventoryDocumentService;

    @Test
    void reportAggregatesOnlyRepositoryCompletedOrdersAcrossAllDimensions() {
        Product mushroom = product(1L, "Nấm hương", "Nấm các loại", "Nông trại A", 5);
        Product carrot = product(2L, "Cà rốt", "Rau củ", null, 0);
        Order january = order("VS-JAN", LocalDateTime.of(2026, 1, 15, 9, 0), "An", "an@example.com",
            new BigDecimal("260000"));
        january.addDetail(detail(mushroom, "Nấm hương", 2, "100000"));
        january.addDetail(detail(carrot, "Cà rốt", 3, "150000"));
        Order april = order("VS-APR", LocalDateTime.of(2026, 4, 2, 14, 0), "Bình", "binh@example.com",
            new BigDecimal("50000"));
        april.addDetail(detail(mushroom, "Nấm hương", 1, "50000"));
        when(orderRepository.findCompletedForReport(any(), any())).thenReturn(List.of(january, april));
        when(productRepository.findAllByOrderByQuantityAscNameAsc()).thenReturn(List.of(carrot, mushroom));
        when(inventoryDocumentService.supplierPurchases(any(), any())).thenReturn(List.of());

        AdminReportView report = service().businessReport(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertEquals(new BigDecimal("310000"), report.totalRevenue());
        assertEquals(2, report.completedOrderCount());
        assertEquals(6, report.unitsSold());
        assertEquals(new BigDecimal("155000"), report.averageOrderValue());
        assertEquals(5, report.totalInventoryUnits());
        assertEquals(1, report.lowStockProductCount());
        assertEquals(1, report.outOfStockProductCount());
        AdminReportView.SalesRow mushroomSales = report.productSales().stream()
            .filter(row -> row.label().equals("Nấm hương"))
            .findFirst()
            .orElseThrow();
        assertEquals(3, mushroomSales.units());
        assertEquals(new BigDecimal("150000"), mushroomSales.revenue());
        assertEquals(List.of("2026-01", "2026-04"),
            report.monthlyRevenue().stream().map(AdminReportView.RevenuePoint::label).toList());
        assertEquals(List.of("2026-Q1", "2026-Q2"),
            report.quarterlyRevenue().stream().map(AdminReportView.RevenuePoint::label).toList());
        assertEquals(new BigDecimal("310000"), report.yearlyRevenue().getFirst().revenue());
        verify(orderRepository).findCompletedForReport(
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    @Test
    void invalidDateRangeIsRejectedBeforeQueryingDatabase() {
        assertThrows(AdminOperationException.class, () -> service().businessReport(
            LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)));
        verify(orderRepository, never()).findCompletedForReport(any(), any());
    }

    @Test
    void csvUsesUtf8BomAndContainsReportSections() {
        when(orderRepository.findCompletedForReport(any(), any())).thenReturn(List.of());
        when(productRepository.findAllByOrderByQuantityAscNameAsc()).thenReturn(List.of());
        when(inventoryDocumentService.supplierPurchases(any(), any())).thenReturn(List.of());
        AdminReportView report = service().businessReport(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        String csv = new String(service().exportBusinessReportCsv(report), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"DOANH SỐ THEO SẢN PHẨM\""));
        assertTrue(csv.contains("\"TỒN KHO\""));
    }

    private AdminService service() {
        return new AdminService(productRepository, categoryRepository, orderRepository, userRepository,
            supplierRepository, brandRepository, eventPublisher,
            org.mockito.Mockito.mock(InventoryService.class), inventoryDocumentService);
    }

    private Product product(Long id, String name, String categoryName, String supplierName, int stock) {
        Category category = new Category();
        category.setName(categoryName);
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory(category);
        product.setQuantity(stock);
        product.setStatus(true);
        if (supplierName != null) {
            Supplier supplier = new Supplier();
            supplier.setName(supplierName);
            product.setSupplier(supplier);
        }
        return product;
    }

    private Order order(
        String code,
        LocalDateTime createdAt,
        String customerName,
        String email,
        BigDecimal total
    ) {
        User user = new User();
        user.setFullName(customerName);
        user.setEmail(email);
        Order order = new Order();
        order.setOrderCode(code);
        order.setStatus(OrderStatus.COMPLETED);
        order.setUser(user);
        order.setTotalAmount(total);
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        return order;
    }

    private OrderDetail detail(Product product, String snapshotName, int quantity, String subtotal) {
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setProductName(snapshotName);
        detail.setQuantity(quantity);
        detail.setSubtotal(new BigDecimal(subtotal));
        return detail;
    }
}
