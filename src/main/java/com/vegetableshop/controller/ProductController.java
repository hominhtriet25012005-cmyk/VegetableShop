package com.vegetableshop.controller;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.entity.Product;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.service.CategoryService;
import com.vegetableshop.service.ProductService;
import com.vegetableshop.service.RecentlyViewedService;
import com.vegetableshop.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("mysql")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final RecentlyViewedService recentlyViewedService;

    public ProductController(
        ProductService productService,
        CategoryService categoryService,
        ReviewService reviewService,
        RecentlyViewedService recentlyViewedService
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
        this.recentlyViewedService = recentlyViewedService;
    }

    @GetMapping("/shop")
    public String shop(@ModelAttribute("filter") ProductFilter filter, Model model) {
        Page<Product> productPage = productService.search(filter);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("featuredProducts", productService.findNewestProducts());
        model.addAttribute("categories", categoryService.findAllActiveCategories());
        return "shop";
    }

    @GetMapping("/product/{id}")
    public String detail(
        @PathVariable Long id,
        Authentication authentication,
        HttpSession session,
        Model model
    ) {
        Product product = productService.findActiveProduct(id);
        String email = authenticatedEmail(authentication);
        ReviewRequest reviewRequest = reviewService.findOwnReview(email, id)
            .map(ReviewRequest::from)
            .orElseGet(ReviewRequest::new);
        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", productService.findRelatedProducts(product));
        model.addAttribute("sameSupplierProducts", productService.findSameSupplierProducts(product));
        model.addAttribute("recentlyViewedProducts", productService.findActiveProductsInOrder(
            recentlyViewedService.recordAndGetPrevious(session, id)));
        model.addAttribute("featuredProducts", productService.findNewestProducts());
        model.addAttribute("categories", categoryService.findAllActiveCategories());
        model.addAttribute("reviews", reviewService.findByProduct(id));
        model.addAttribute("reviewSummary", reviewService.summarize(id));
        model.addAttribute("canReview", reviewService.canReview(email, id));
        model.addAttribute("reviewRequest", reviewRequest);
        return "shop-detail";
    }

    @PostMapping("/product/{id}/reviews")
    public String saveReview(
        @PathVariable Long id,
        Authentication authentication,
        @Valid @ModelAttribute ReviewRequest request,
        BindingResult bindingResult,
        RedirectAttributes attributes
    ) {
        if (bindingResult.hasErrors()) {
            attributes.addFlashAttribute("errorMessage",
                bindingResult.getAllErrors().getFirst().getDefaultMessage());
            return "redirect:/product/" + id + "#reviews";
        }
        try {
            reviewService.save(authentication.getName(), id, request);
            attributes.addFlashAttribute("successMessage", "Đã lưu đánh giá của bạn");
        } catch (ReviewOperationException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/product/" + id + "#reviews";
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }
}
