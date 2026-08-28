package com.vegetableshop.controller;

import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartApiControllerTests {

    @Mock CartService cartService;

    @Test
    void addUsesAuthenticatedEmailAndReturnsServerTotals() {
        CartMutationResponse response = response("Đã thêm sản phẩm vào giỏ hàng");
        when(cartService.addProduct("user@example.com", 3L, 2)).thenReturn(response);

        CartMutationResponse actual = controller().addItem(authentication(), 3L, 2);

        assertEquals(response, actual);
        verify(cartService).addProduct("user@example.com", 3L, 2);
    }

    @Test
    void invalidStockReturnsBadRequestWithFriendlyMessage() {
        CartOperationException error = new CartOperationException("Sản phẩm chỉ còn 2 sản phẩm");

        var result = controller().cartError(error);

        assertEquals(400, result.getStatusCode().value());
        assertEquals(error.getMessage(), result.getBody().message());
    }

    private CartApiController controller() {
        return new CartApiController(cartService);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
            "user@example.com", "ignored", List.of());
    }

    private CartMutationResponse response(String message) {
        return new CartMutationResponse(8L, 2, 10, new BigDecimal("70000"),
            4, new BigDecimal("140000"), false, message);
    }
}
