package com.vegetableshop.service;

import com.vegetableshop.dto.AdminCategoryRequest;
import com.vegetableshop.dto.AdminProductRequest;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderDetail;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.CategoryRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import com.vegetableshop.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private SupplierRepository supplierRepository;

    @Test
    void dashboardUsesCompletedRevenueAndDatabaseCounts() {
        when(productRepository.count()).thenReturn(5L);
        when(categoryRepository.count()).thenReturn(3L);
        when(userRepository.count()).thenReturn(7L);
        when(orderRepository.count()).thenReturn(4L);
        when(orderRepository.calculateRevenueByStatus(OrderStatus.COMPLETED))
            .thenReturn(new BigDecimal("250000"));
        when(orderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(productRepository.findTop5ByStatusTrueAndQuantityLessThanEqualOrderByQuantityAsc(10))
            .thenReturn(List.of());

        var dashboard = service().dashboard();

        assertEquals(5L, dashboard.productCount());
        assertEquals(new BigDecimal("250000"), dashboard.revenue());
    }

    @Test
    void createProductMapsValidatedRequestAndCategory() {
        Category category = category(2L, "Rau củ");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product product = service().createProduct(productRequest());

        assertEquals("Cà rốt", product.getName());
        assertEquals(new BigDecimal("25000"), product.getPrice());
        assertEquals(category, product.getCategory());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findProductsUsesCombinedAdminFiltersAndKeepsRequestedPage() {
        Product product = product(8L, 12);
        var resultPage = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(resultPage);

        var result = service().findProducts("  Cam  ", 3L, false, 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertSame(resultPage, result);
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void duplicateCategoryNameIsRejected() {
        Category existing = category(1L, "Rau củ");
        when(categoryRepository.findByNameIgnoreCase("Rau củ")).thenReturn(Optional.of(existing));
        AdminCategoryRequest request = new AdminCategoryRequest();
        request.setName("Rau củ");

        assertThrows(AdminOperationException.class, () -> service().createCategory(request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void cancellingPendingOrderRestoresEveryProductStock() {
        Product first = product(1L, 4);
        Product second = product(2L, 8);
        Order order = order(OrderStatus.PENDING);
        order.addDetail(detail(first, 3));
        order.addDetail(detail(second, 2));
        when(orderRepository.findAdminById(9L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(first));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(second));

        service().updateOrderStatus(9L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(7, first.getQuantity());
        assertEquals(10, second.getQuantity());
    }

    @Test
    void completingCodOrderMarksItPaid() {
        Order order = order(OrderStatus.SHIPPING);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        when(orderRepository.findAdminById(6L)).thenReturn(Optional.of(order));

        service().updateOrderStatus(6L, OrderStatus.COMPLETED);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertNotNull(order.getPaidAt());
    }

    @Test
    void orderCannotSkipWorkflowSteps() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findAdminById(5L)).thenReturn(Optional.of(order));

        assertThrows(AdminOperationException.class,
            () -> service().updateOrderStatus(5L, OrderStatus.COMPLETED));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void adminAccountCannotBeLocked() {
        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        admin.setStatus(true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

        assertThrows(AdminOperationException.class,
            () -> service().toggleUserStatus(3L, "other-admin@example.com"));
        assertTrue(admin.isStatus());
    }

    @Test
    void normalUserCanBeLocked() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        user.setStatus(true);
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        boolean active = service().toggleUserStatus(4L, "admin@example.com");

        assertEquals(false, active);
        assertEquals(false, user.isStatus());
    }

    private AdminService service() {
        return new AdminService(productRepository, categoryRepository, orderRepository, userRepository,
            supplierRepository);
    }

    private AdminProductRequest productRequest() {
        AdminProductRequest request = new AdminProductRequest();
        request.setName(" Cà rốt ");
        request.setPrice(new BigDecimal("25000"));
        request.setQuantity(10);
        request.setCategoryId(2L);
        request.setStatus(true);
        return request;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private Product product(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setQuantity(stock);
        return product;
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        return order;
    }

    private OrderDetail detail(Product product, int quantity) {
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(quantity);
        return detail;
    }
}
