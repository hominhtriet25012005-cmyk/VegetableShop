package com.vegetableshop.controller;

import com.vegetableshop.service.CartService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Profile("mysql")
public class CartModelAdvice {

    private final CartService cartService;

    public CartModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        return cartService.countTotalQuantity(authentication.getName());
    }
}
