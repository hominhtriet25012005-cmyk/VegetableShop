package com.vegetableshop.controller;

import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.service.CartService;
import com.vegetableshop.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistApiControllerTests {

    @Mock WishlistService wishlistService;
    @Mock CartService cartService;

    @Test
    void addIsIdempotentAndReturnsServerCounts() {
        when(wishlistService.add("user@example.com", 7L)).thenReturn(false);
        when(wishlistService.countActive("user@example.com")).thenReturn(3L);
        when(cartService.countTotalQuantity("user@example.com")).thenReturn(2);

        var response = controller().add(authentication(), 7L);

        assertEquals(7L, response.productId());
        assertEquals(3L, response.wishlistCount());
        assertEquals("Sản phẩm đã có trong danh sách yêu thích", response.message());
    }

    @Test
    void moveAddsToCartThenRemovesOwnedWishlistItem() {
        when(cartService.addProduct("user@example.com", 4L, 1)).thenReturn(
            new CartMutationResponse(9L, 1, 10, new BigDecimal("35000"),
                5, new BigDecimal("120000"), false, "Đã thêm"));
        when(wishlistService.countActive("user@example.com")).thenReturn(1L);

        var response = controller().moveToCart(authentication(), 4L, 1);

        assertFalse(response.wishlisted());
        assertEquals(5, response.cartTotalQuantity());
        verify(wishlistService).remove("user@example.com", 4L);
    }

    private WishlistApiController controller() {
        return new WishlistApiController(wishlistService, cartService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
            "user@example.com", "ignored", List.of());
    }
}
