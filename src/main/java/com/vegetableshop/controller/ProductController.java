package com.vegetableshop.controller;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.entity.Product;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.service.CategoryService;
import com.vegetableshop.service.ProductService;
import com.vegetableshop.service.ProductViewHistoryService;
import com.vegetableshop.service.RecommendationService;
import com.vegetableshop.service.RecentlyViewedService;
import com.vegetableshop.service.ReviewService;
import com.vegetableshop.service.WishlistService;
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
    private final ProductViewHistoryService productViewHistoryService;
    private final RecommendationService recommendationService;
    private final WishlistService wishlistService;

    public ProductController(
        ProductService productService,
        CategoryService categoryService,
        ReviewService reviewService,
        RecentlyViewedService recentlyViewedService,
        ProductViewHistoryService productViewHistoryService,
        RecommendationService recommendationService,
        WishlistService wishlistService
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
        this.recentlyViewedService = recentlyViewedService;
        this.productViewHistoryService = productViewHistoryService;
        this.recommendationService = recommendationService;
        this.wishlistService = wishlistService;
    }

    @GetMapping("/shop")
    public String shop(
        @ModelAttribute("filter") ProductFilter filter,
        Authentication authentication,
        Model model
    ) {
        Page<Product> productPage = productService.search(filter);
        String email = authenticatedEmail(authentication);
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("wishlistProductIds", wishlistService.findActiveProductIds(email));
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
        java.util.List<Product> recentlyViewedProducts = email == null
            ? productService.findActiveProductsInOrder(recentlyViewedService.recordAndGetPrevious(session, id))
            : productViewHistoryService.recordAndFindPrevious(email, id);
        ReviewRequest reviewRequest = new ReviewRequest();
        var eligiblePurchases = reviewService.eligiblePurchases(email, id);
        model.addAttribute("eligiblePurchases", eligiblePurchases);
        model.addAttribute("ownReviews", reviewService.findOwnReviews(email, id));
        model.addAttribute("product", product);
        model.addAttribute("productImages", productService.galleryImages(product));
        model.addAttribute("relatedProducts", productService.findRelatedProducts(product));
        model.addAttribute("sameBrandProducts", productService.findSameBrandProducts(product));
        model.addAttribute("recentlyViewedProducts", recentlyViewedProducts);
        model.addAttribute("recommendedProducts", recommendationService.recommend(product, recentlyViewedProducts));
        model.addAttribute("wishlisted", wishlistService.contains(email, id));
        model.addAttribute("wishlistProductIds", wishlistService.findActiveProductIds(email));
        model.addAttribute("featuredProducts", productService.findNewestProducts());
        model.addAttribute("categories", categoryService.findAllActiveCategories());
        model.addAttribute("reviews", reviewService.findByProduct(id));
        model.addAttribute("reviewSummary", reviewService.summarize(id));
        model.addAttribute("canReview", !eligiblePurchases.isEmpty());
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
            attributes.addFlashAttribute("successMessage", "Đã gửi đánh giá. Nội dung và ảnh sẽ hiển thị sau khi Admin duyệt");
        } catch (ReviewOperationException exception) {
            attributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            attributes.addFlashAttribute("errorMessage", "Không lưu được đánh giá: đơn có thể đã được đánh giá hoặc database chưa nâng cấp giai đoạn 19");
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
