package com.vegetableshop.controller;

import com.vegetableshop.dto.CartApiError;
import com.vegetableshop.dto.CartMutationResponse;
import com.vegetableshop.dto.WishlistActionResponse;
import com.vegetableshop.exception.CartOperationException;
import com.vegetableshop.exception.ProductNotFoundException;
import com.vegetableshop.exception.WishlistOperationException;
import com.vegetableshop.service.CartService;
import com.vegetableshop.service.WishlistService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mysql")
public class WishlistApiController {

    private final WishlistService wishlistService;
    private final CartService cartService;

    public WishlistApiController(WishlistService wishlistService, CartService cartService) {
        this.wishlistService = wishlistService;
        this.cartService = cartService;
    }

    @PostMapping("/api/wishlist/items")
    public WishlistActionResponse add(Authentication authentication, @RequestParam Long productId) {
        String email = authentication.getName();
        boolean created = wishlistService.add(email, productId);
        return response(email, productId, true,
            created ? "Đã thêm vào sản phẩm yêu thích" : "Sản phẩm đã có trong danh sách yêu thích");
    }

    @DeleteMapping("/api/wishlist/items/{productId}")
    public WishlistActionResponse remove(Authentication authentication, @PathVariable Long productId) {
        String email = authentication.getName();
        wishlistService.remove(email, productId);
        return response(email, productId, false, "Đã xóa khỏi sản phẩm yêu thích");
    }

    @PostMapping("/api/wishlist/items/{productId}/move-to-cart")
    public WishlistActionResponse moveToCart(
        Authentication authentication,
        @PathVariable Long productId,
        @RequestParam(defaultValue = "1") int quantity
    ) {
        String email = authentication.getName();
        CartMutationResponse cart = cartService.addProduct(email, productId, quantity);
        wishlistService.remove(email, productId);
        return new WishlistActionResponse(
            productId,
            false,
            wishlistService.countActive(email),
            cart.totalQuantity(),
            "Đã chuyển sản phẩm sang giỏ hàng"
        );
    }

    @ExceptionHandler({
        WishlistOperationException.class,
        CartOperationException.class,
        ProductNotFoundException.class
    })
    public ResponseEntity<CartApiError> wishlistError(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new CartApiError(exception.getMessage()));
    }

    private WishlistActionResponse response(
        String email,
        Long productId,
        boolean wishlisted,
        String message
    ) {
        return new WishlistActionResponse(
            productId,
            wishlisted,
            wishlistService.countActive(email),
            cartService.countTotalQuantity(email),
            message
        );
    }
}
