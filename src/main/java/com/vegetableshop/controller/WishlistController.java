package com.vegetableshop.controller;

import com.vegetableshop.service.WishlistService;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("mysql")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping("/wishlist")
    public String wishlist(Authentication authentication, Model model) {
        model.addAttribute("wishlistItems", wishlistService.findActiveItems(authentication.getName()));
        return "wishlist";
    }
}
