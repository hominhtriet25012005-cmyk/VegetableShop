package com.vegetableshop.controller;

import com.vegetableshop.dto.CartApiError;
import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.service.CartService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
public class CartApiController {

    private final CartService cartService;

    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/api/cart/items")
    public CartMutationResponse addItem(
        Authentication authentication,
        @RequestParam Long productId,
        @RequestParam(defaultValue = "1") int quantity
    ) {
        return cartService.addProduct(authentication.getName(), productId, quantity);
    }

    @PatchMapping("/api/cart/items/{itemId}")
    public CartMutationResponse updateQuantity(
        Authentication authentication,
        @PathVariable Long itemId,
        @RequestParam int quantity
    ) {
        return cartService.updateQuantity(authentication.getName(), itemId, quantity);
    }

    @DeleteMapping("/api/cart/items/{itemId}")
    public CartMutationResponse deleteItem(Authentication authentication, @PathVariable Long itemId) {
        return cartService.removeItem(authentication.getName(), itemId);
    }

    @ExceptionHandler({CartOperationException.class, ProductNotFoundException.class})
    public ResponseEntity<CartApiError> cartError(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new CartApiError(exception.getMessage()));
    }
}
