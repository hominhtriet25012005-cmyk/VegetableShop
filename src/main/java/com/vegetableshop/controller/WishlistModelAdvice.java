package com.vegetableshop.controller;

import com.vegetableshop.service.WishlistService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Profile("mysql")
public class WishlistModelAdvice {

    private final WishlistService wishlistService;

    public WishlistModelAdvice(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @ModelAttribute("wishlistItemCount")
    public long wishlistItemCount(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        return wishlistService.countActive(authentication.getName());
    }
}
