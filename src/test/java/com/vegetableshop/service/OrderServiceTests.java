package com.vegetableshop.service;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.dto.CheckoutPricingView;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.entity.Voucher;
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.exception.OrderNotFoundException;
import com.vegetableshop.exception.OrderOperationException;
import com.vegetableshop.repository.CartRepository;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ProductRepository;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private InventoryService inventoryService;

    @Test
    void placeOrderSnapshotsPricesDecrementsStockAndClearsCart() {
        User user = activeUser();
        Product carrot = product(1L, "Cà rốt", "25000", 10);
        Product orange = product(2L, "Cam", "40000", 8);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.addItem(item(carrot, 2));
        cart.addItem(item(orange, 1));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findForCheckout("user@example.com")).thenReturn(Optional.of(cart));
        when(productRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(carrot));
        when(productRepository.findActiveByIdForUpdate(2L)).thenReturn(Optional.of(orange));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        Order saved = service().placeOrder("user@example.com", validRequest());

        assertEquals(99L, saved.getId());
        assertEquals(new BigDecimal("90000"), saved.getTotalAmount());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
        assertEquals(PaymentMethod.COD, saved.getPaymentMethod());
        assertEquals(PaymentStatus.UNPAID, saved.getPaymentStatus());
        assertEquals(2, saved.getDetails().size());
        assertEquals("Cà rốt", saved.getDetails().getFirst().getProductName());
        assertEquals(new BigDecimal("25000"), saved.getDetails().getFirst().getPrice());
        assertEquals(8, carrot.getQuantity());
        assertEquals(7, orange.getQuantity());
        assertTrue(cart.getItems().isEmpty());
        verify(inventoryService).recordSale(carrot, 10, 2, saved.getOrderCode(), "user@example.com");
        verify(inventoryService).recordSale(orange, 8, 1, saved.getOrderCode(), "user@example.com");
        verify(eventPublisher).publishEvent(any(OrderPlacedMailEvent.class));
    }

    @Test
    void placeOrderRejectsInsufficientStockBeforeSaving() {
        User user = activeUser();
        Product product = product(4L, "Bắp cải", "30000", 2);
        Cart cart = new Cart();
        cart.addItem(item(product, 3));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findForCheckout("user@example.com")).thenReturn(Optional.of(cart));
        when(productRepository.findActiveByIdForUpdate(4L)).thenReturn(Optional.of(product));

        assertThrows(OrderOperationException.class,
            () -> service().placeOrder("user@example.com", validRequest()));
        verify(orderRepository, never()).save(any(Order.class));
        assertEquals(2, product.getQuantity());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void placeOrderPersistsDiscountSnapshotsAndVoucherUsage() {
        User user = activeUser();
        Product carrot = product(1L, "Cà rốt", "100000", 5);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.addItem(item(carrot, 2));
        Voucher voucher = new Voucher();
        voucher.setId(12L);
        voucher.setCode("SAVE20");
        CheckoutPricingService pricingService = org.mockito.Mockito.mock(CheckoutPricingService.class);
        VoucherService voucherService = org.mockito.Mockito.mock(VoucherService.class);
        CheckoutPricingView.Line line = new CheckoutPricingView.Line(
            carrot, 2, new BigDecimal("100000"), new BigDecimal("90000"),
            new BigDecimal("20000"), new BigDecimal("18000"), new BigDecimal("162000"),
            "Khuyến mãi rau củ", false
        );
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findForCheckout("user@example.com")).thenReturn(Optional.of(cart));
        when(productRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(carrot));
        when(pricingService.quote(
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq(user),
            org.mockito.ArgumentMatchers.eq("SAVE20"),
            org.mockito.ArgumentMatchers.eq(true)
        )).thenReturn(new CheckoutPricingView(
            java.util.List.of(line), new BigDecimal("200000"), new BigDecimal("20000"),
            new BigDecimal("18000"), new BigDecimal("162000"), voucher, "Đã áp dụng SAVE20"
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(88L);
            return order;
        });
        CheckoutRequest request = validRequest();
        request.setVoucherCode("SAVE20");
        OrderService orderService = new OrderService(
            cartRepository, productRepository, userRepository, orderRepository, eventPublisher,
            inventoryService, org.mockito.Mockito.mock(BankTransferService.class),
            Optional.of(pricingService), Optional.of(voucherService)
        );

        Order saved = orderService.placeOrder("user@example.com", request);

        assertEquals(new BigDecimal("200000"), saved.getSubtotalAmount());
        assertEquals(new BigDecimal("20000"), saved.getPromotionDiscountAmount());
        assertEquals(new BigDecimal("18000"), saved.getVoucherDiscountAmount());
        assertEquals(new BigDecimal("162000"), saved.getTotalAmount());
        assertEquals("SAVE20", saved.getVoucherCode());
        assertEquals(new BigDecimal("38000"), saved.getDetails().getFirst().getDiscountAmount());
        verify(voucherService).record(voucher, user, saved, new BigDecimal("18000"));
    }

    @Test
    void emptyCartCannotCreateOrder() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findForCheckout("user@example.com")).thenReturn(Optional.of(new Cart()));
        assertThrows(OrderOperationException.class,
            () -> service().placeOrder("user@example.com", validRequest()));
    }

    @Test
    void orderDetailIsAlwaysLoadedByOwnerEmail() {
        when(orderRepository.findByIdAndUserEmailIgnoreCase(8L, "owner@example.com"))
            .thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class,
            () -> service().findOwnedOrder(8L, "owner@example.com"));
        verify(orderRepository).findByIdAndUserEmailIgnoreCase(8L, "owner@example.com");
    }

    private OrderService service() {
        return new OrderService(cartRepository, productRepository, userRepository, orderRepository,
            eventPublisher, inventoryService, org.mockito.Mockito.mock(BankTransferService.class));
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(true);
        user.setEmail("user@example.com");
        user.setFullName("Nguyễn Văn An");
        return user;
    }

    private Product product(Long id, String name, String price, int stock) {
        Category category = new Category();
        category.setStatus(true);
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setQuantity(stock);
        product.setStatus(true);
        product.setCategory(category);
        return product;
    }

    private CartItem item(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private CheckoutRequest validRequest() {
        CheckoutRequest request = new CheckoutRequest();
        request.setReceiverName("Nguyễn Văn An");
        request.setReceiverPhone("0901234567");
        request.setShippingAddress("123 Nguyễn Trãi");
        request.setPaymentMethod("COD");
        return request;
    }
}
