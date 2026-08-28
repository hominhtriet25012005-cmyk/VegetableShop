package com.vegetableshop.controller;

import com.vegetableshop.entity.Cart;
import com.vegetableshop.entity.CartItem;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTests {

    @Mock
    private CartService cartService;

    @Test
    void cartAddsItemsAndBackendTotalToModel() {
        Cart cart = new Cart();
        CartItem item = new CartItem();
        cart.addItem(item);
        when(cartService.getOrCreateCart("user@example.com")).thenReturn(cart);
        when(cartService.calculateTotal(cart)).thenReturn(new BigDecimal("70000"));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller().cart(authentication(), model);

        assertEquals("cart", view);
        assertSame(cart, model.getAttribute("cart"));
        assertEquals(List.of(item), model.getAttribute("cartItems"));
        assertEquals(new BigDecimal("70000"), model.getAttribute("cartTotal"));
    }

    @Test
    void addItemUsesAuthenticatedEmailAndRedirectsToCart() {
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller().addItem(authentication(), 3L, 2, attributes);

        verify(cartService).addProduct("user@example.com", 3L, 2);
        assertEquals("redirect:/cart", view);
        assertEquals("Đã thêm sản phẩm vào giỏ hàng", attributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    void updateQuantityReturnsFriendlyStockError() {
        doThrow(new CartOperationException("Chỉ còn 3 sản phẩm"))
            .when(cartService).updateQuantity("user@example.com", 5L, 4);
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();

        String view = controller().updateQuantity(authentication(), 5L, 4, attributes);

        assertEquals("redirect:/cart", view);
        assertEquals("Chỉ còn 3 sản phẩm", attributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void deleteItemUsesAuthenticatedOwner() {
        controller().deleteItem(authentication(), 8L, new RedirectAttributesModelMap());

        verify(cartService).removeItem("user@example.com", 8L);
    }

    private CartController controller() {
        return new CartController(cartService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated("user@example.com", "ignored", List.of());
    }
}
