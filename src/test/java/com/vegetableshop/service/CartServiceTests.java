package com.vegetableshop.service;

import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.User;
import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.repository.CartItemRepository;
import com.vegetableshop.repository.CartRepository;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTests {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    void getOrCreateCartCreatesOneCartForActiveUser() {
        User user = new User();
        user.setStatus(true);
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = service().getOrCreateCart("user@example.com");

        assertSame(user, cart.getUser());
        verify(cartRepository).save(cart);
    }

    @Test
    void addProductCreatesNewItemWithRequestedQuantity() {
        Product product = product(5);
        Cart cart = cart(10L);
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(2L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 2L)).thenReturn(Optional.empty());

        service().addProduct("user@example.com", 2L, 2);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).saveAndFlush(captor.capture());
        assertEquals(2, captor.getValue().getQuantity());
        assertSame(product, captor.getValue().getProduct());
        assertSame(cart, captor.getValue().getCart());
    }

    @Test
    void addProductAddsToExistingQuantity() {
        Product product = product(10);
        Cart cart = cart(10L);
        CartItem item = item(7L, cart, product, 3);
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(2L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 2L)).thenReturn(Optional.of(item));
        when(cartItemRepository.sumQuantityByUserEmail("user@example.com")).thenReturn(7L);
        when(cartItemRepository.sumSubtotalByUserEmail("user@example.com"))
            .thenReturn(new BigDecimal("70000"));
        when(cartItemRepository.countByCartUserEmailIgnoreCase("user@example.com")).thenReturn(1L);

        CartMutationResponse response = service().addProduct("user@example.com", 2L, 4);

        assertEquals(7, item.getQuantity());
        assertEquals(7, response.totalQuantity());
        assertEquals(new BigDecimal("70000"), response.cartTotal());
        assertEquals(false, response.empty());
        verify(cartItemRepository).saveAndFlush(item);
    }

    @Test
    void addProductRejectsQuantityAboveStock() {
        Product product = product(3);
        Cart cart = cart(10L);
        when(productRepository.findByIdAndStatusTrueAndCategoryStatusTrue(2L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(
            CartOperationException.class,
            () -> service().addProduct("user@example.com", 2L, 4)
        );
        verify(cartItemRepository, never()).saveAndFlush(any(CartItem.class));
    }

    @Test
    void updateQuantityFindsItemByIdAndOwnerEmail() {
        Product product = product(8);
        CartItem item = item(4L, cart(10L), product, 1);
        when(cartItemRepository.findByIdAndCartUserEmailIgnoreCase(4L, "owner@example.com"))
            .thenReturn(Optional.of(item));

        service().updateQuantity("owner@example.com", 4L, 6);

        assertEquals(6, item.getQuantity());
        verify(cartItemRepository).saveAndFlush(item);
    }

    @Test
    void updateAndDeleteRejectItemOwnedByAnotherUser() {
        when(cartItemRepository.findByIdAndCartUserEmailIgnoreCase(99L, "attacker@example.com"))
            .thenReturn(Optional.empty());

        assertThrows(
            CartOperationException.class,
            () -> service().updateQuantity("attacker@example.com", 99L, 1)
        );
        assertThrows(
            CartOperationException.class,
            () -> service().removeItem("attacker@example.com", 99L)
        );
    }

    @Test
    void calculateTotalUsesCurrentProductPrices() {
        Cart cart = cart(10L);
        Product first = product(10);
        first.setPrice(new BigDecimal("35000"));
        Product second = product(10);
        second.setPrice(new BigDecimal("55000"));
        cart.addItem(item(1L, cart, first, 2));
        cart.addItem(item(2L, cart, second, 1));

        assertEquals(new BigDecimal("125000"), service().calculateTotal(cart));
    }

    private CartService service() {
        return new CartService(cartRepository, cartItemRepository, productRepository, userRepository);
    }

    private Cart cart(Long id) {
        Cart cart = new Cart();
        cart.setId(id);
        return cart;
    }

    private Product product(int stock) {
        Category category = new Category();
        category.setStatus(true);
        Product product = new Product();
        product.setName("Cam");
        product.setPrice(new BigDecimal("10000"));
        product.setQuantity(stock);
        product.setStatus(true);
        product.setCategory(category);
        return product;
    }

    private CartItem item(Long id, Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
