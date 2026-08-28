package com.vegetableshop.service;

import com.vegetableshop.dto.CheckoutRequest;
import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Order;
import com.vegetableshop.entity.OrderStatus;
import com.vegetableshop.entity.PaymentMethod;
import com.vegetableshop.entity.PaymentStatus;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
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
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(cart));
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
    }

    @Test
    void placeOrderRejectsInsufficientStockBeforeSaving() {
        User user = activeUser();
        Product product = product(4L, "Bắp cải", "30000", 2);
        Cart cart = new Cart();
        cart.addItem(item(product, 3));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(cart));
        when(productRepository.findActiveByIdForUpdate(4L)).thenReturn(Optional.of(product));

        assertThrows(OrderOperationException.class,
            () -> service().placeOrder("user@example.com", validRequest()));
        verify(orderRepository, never()).save(any(Order.class));
        assertEquals(2, product.getQuantity());
        assertEquals(1, cart.getItems().size());
    }

    @Test
    void emptyCartCannotCreateOrder() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(new Cart()));
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
        return new OrderService(cartRepository, productRepository, userRepository, orderRepository);
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(true);
        user.setEmail("user@example.com");
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
