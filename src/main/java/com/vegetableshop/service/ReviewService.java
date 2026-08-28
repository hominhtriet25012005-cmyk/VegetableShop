package com.vegetableshop.service;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.dto.ReviewSummary;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Review;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ReviewRepository;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Profile("mysql")
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;

    public ReviewService(
        ReviewRepository reviewRepository,
        UserRepository userRepository,
        OrderRepository orderRepository,
        ProductService productService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<Review> findByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public ReviewSummary summarize(Long productId) {
        Object[] values = reviewRepository.summarize(productId);
        if (values == null || values.length < 2 || !(values[1] instanceof Number count)) {
            return ReviewSummary.empty();
        }
        double average = values[0] instanceof Number number ? number.doubleValue() : 0.0;
        return new ReviewSummary(average, count.longValue());
    }

    @Transactional(readOnly = true)
    public boolean canReview(String email, Long productId) {
        return email != null && orderRepository.hasCompletedPurchase(email, productId);
    }

    @Transactional(readOnly = true)
    public Optional<Review> findOwnReview(String email, Long productId) {
        if (email == null) {
            return Optional.empty();
        }
        return reviewRepository.findByUserEmailIgnoreCaseAndProductId(email, productId);
    }

    @Transactional
    public Review save(String email, Long productId, ReviewRequest request) {
        if (!orderRepository.hasCompletedPurchase(email, productId)) {
            throw new ReviewOperationException("Bạn chỉ có thể đánh giá sản phẩm đã mua và nhận hàng thành công");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
            .filter(User::isStatus)
            .orElseThrow(() -> new ReviewOperationException("Tài khoản không còn hoạt động"));
        Product product = productService.findActiveProduct(productId);
        Review review = reviewRepository.findByUserEmailIgnoreCaseAndProductId(email, productId)
            .orElseGet(Review::new);
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(normalizeComment(request.getComment()));
        return reviewRepository.save(review);
    }

    private String normalizeComment(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
