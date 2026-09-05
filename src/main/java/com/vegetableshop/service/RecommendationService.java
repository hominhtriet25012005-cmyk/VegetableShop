package com.vegetableshop.service;

import com.vegetableshop.dto.ProductRecommendation;
import com.vegetableshop.entity.Product;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Profile("mysql")
public class RecommendationService {

    private static final int LIMIT = 8;
    private final ProductService productService;

    public RecommendationService(ProductService productService) {
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<ProductRecommendation> recommend(Product current, List<Product> recentlyViewed) {
        List<Product> history = recentlyViewed == null ? List.of() : recentlyViewed;
        return productService.findAllActiveProducts().stream()
            .filter(candidate -> !candidate.getId().equals(current.getId()))
            .filter(Product::isStatus)
            .filter(candidate -> candidate.getCategory().isStatus())
            .filter(candidate -> candidate.getQuantity() != null && candidate.getQuantity() > 0)
            .map(candidate -> recommendation(current, candidate, history))
            .filter(recommendation -> recommendation.score() > 0)
            .sorted(Comparator.comparingInt(ProductRecommendation::score).reversed()
                .thenComparing(item -> priceDistance(current, item.product()))
                .thenComparing(item -> item.product().getId()))
            .limit(LIMIT)
            .toList();
    }

    private ProductRecommendation recommendation(Product current, Product candidate, List<Product> history) {
        boolean sameCategory = sameId(current.getCategory().getId(), candidate.getCategory().getId());
        boolean sameBrand = current.getBrand() != null
            && candidate.getBrand() != null
            && current.getBrand().isStatus()
            && candidate.getBrand().isStatus()
            && sameId(current.getBrand().getId(), candidate.getBrand().getId());
        boolean nearPrice = isNearPrice(current, candidate);
        boolean historyAffinity = history.stream().anyMatch(viewed ->
            sameId(viewed.getCategory().getId(), candidate.getCategory().getId())
                || viewed.getBrand() != null && candidate.getBrand() != null
                && viewed.getBrand().isStatus() && candidate.getBrand().isStatus()
                && sameId(viewed.getBrand().getId(), candidate.getBrand().getId()));

        int score = (sameCategory ? 4 : 0)
            + (sameBrand ? 3 : 0)
            + (nearPrice ? 2 : 0)
            + (historyAffinity ? 2 : 0);
        String reason = reason(sameCategory, sameBrand, nearPrice, historyAffinity);
        return new ProductRecommendation(candidate, score, reason);
    }

    private String reason(
        boolean sameCategory,
        boolean sameBrand,
        boolean nearPrice,
        boolean historyAffinity
    ) {
        if (sameCategory && sameBrand) {
            return "Cùng danh mục và thương hiệu";
        }
        if (historyAffinity) {
            return "Phù hợp lịch sử bạn đã xem";
        }
        if (sameCategory) {
            return "Cùng danh mục";
        }
        if (sameBrand) {
            return "Cùng thương hiệu";
        }
        return nearPrice ? "Mức giá tương tự" : "Có thể bạn quan tâm";
    }

    private boolean isNearPrice(Product left, Product right) {
        if (left.getPrice() == null || right.getPrice() == null || left.getPrice().signum() <= 0) {
            return false;
        }
        BigDecimal threshold = left.getPrice().multiply(new BigDecimal("0.20"));
        return priceDistance(left, right).compareTo(threshold) <= 0;
    }

    private BigDecimal priceDistance(Product left, Product right) {
        if (left.getPrice() == null || right.getPrice() == null) {
            return BigDecimal.valueOf(Long.MAX_VALUE);
        }
        return left.getPrice().subtract(right.getPrice()).abs();
    }

    private boolean sameId(Long left, Long right) {
        return left != null && left.equals(right);
    }
}
