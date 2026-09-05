package com.vegetableshop.controller;

import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.service.CategoryService;
import com.vegetableshop.service.ProductService;
import com.vegetableshop.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

/**
 * Handles the public pages migrated from the original static template.
 *
 * <p>Product listing and detail pages are handled by ProductController. The
 * remaining mappings are static placeholders for later phases.</p>
 */
@Controller
public class HomeController {

    private final Optional<ProductService> productService;
    private final Optional<CategoryService> categoryService;
    private final Optional<WishlistService> wishlistService;

    public HomeController(Optional<ProductService> productService,
                          Optional<CategoryService> categoryService,
                          Optional<WishlistService> wishlistService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.wishlistService = wishlistService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        List<Category> categories = categoryService
            .map(CategoryService::findAllActiveCategories)
            .orElseGet(List::of);
        List<Product> homeProducts = productService
            .map(service -> service.findHomepageCatalog(categories))
            .orElseGet(List::of);

        model.addAttribute("homeProducts", homeProducts);
        String email = authentication == null || "anonymousUser".equals(authentication.getName())
            ? null : authentication.getName();
        model.addAttribute("wishlistProductIds", wishlistService
            .map(service -> service.findActiveProductIds(email))
            .orElseGet(java.util.Set::of));
        model.addAttribute("homeFeaturedCount", Math.min(homeProducts.size(), 8));
        model.addAttribute("categories", categories);
        return "index";
    }

    @GetMapping("/shop-detail")
    public String shopDetail() {
        return "redirect:/shop";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/news")
    public String news() {
        return "news";
    }

    @GetMapping("/testimonial")
    public String testimonial() {
        return "testimonial";
    }
}
