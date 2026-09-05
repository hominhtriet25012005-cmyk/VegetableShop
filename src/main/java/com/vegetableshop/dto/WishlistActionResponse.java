package com.vegetableshop.dto;

public record WishlistActionResponse(
    Long productId,
    boolean wishlisted,
    long wishlistCount,
    int cartTotalQuantity,
    String message
) {
}
