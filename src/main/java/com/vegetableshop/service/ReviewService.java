package com.vegetableshop.service;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.dto.ReviewSummary;
import com.vegetableshop.entity.*;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;

@Service
@Profile("mysql")
public class ReviewService {
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final OrderDetailRepository details;
    private final ReviewImageStorage storage;

    public ReviewService(ReviewRepository reviews, UserRepository users,
                         OrderDetailRepository details, ReviewImageStorage storage) {
        this.reviews = reviews; this.users = users; this.details = details; this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<Review> findByProduct(Long productId) { return reviews.findPublished(productId); }

    @Transactional(readOnly = true)
    public ReviewSummary summarize(Long productId) {
        Map<Integer, Long> counts = new HashMap<>();
        long total = 0, weighted = 0;
        for (Object[] row : reviews.countPublishedByRating(productId)) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            counts.put(star, count); total += count; weighted += star * count;
        }
        return new ReviewSummary(total == 0 ? 0 : (double) weighted / total, total, Map.copyOf(counts));
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> eligiblePurchases(String email, Long productId) {
        return email == null ? List.of() : details.findReviewable(email, productId);
    }

    @Transactional(readOnly = true)
    public boolean canReview(String email, Long productId) { return !eligiblePurchases(email, productId).isEmpty(); }

    @Transactional(readOnly = true)
    public List<Review> findOwnReviews(String email, Long productId) {
        return email == null ? List.of() : reviews.findByUserEmailIgnoreCaseAndProductIdOrderByCreatedAtDesc(email, productId);
    }

    @Transactional
    public Review save(String email, Long productId, ReviewRequest request) {
        if (request.getOrderDetailId() == null || request.getRating() == null
            || request.getRating() < 1 || request.getRating() > 5
            || (request.getComment() != null && request.getComment().length() > 1000))
            throw invalid("Vui lòng chọn đơn hàng, số sao từ 1–5 và nhận xét tối đa 1000 ký tự");
        User user = users.findByEmailIgnoreCase(email).filter(User::isStatus)
            .orElseThrow(() -> invalid("Tài khoản không còn hoạt động"));
        // Serialize submissions for the same purchase; the unique key is a second guard.
        OrderDetail detail = details.lockForReview(request.getOrderDetailId())
            .orElseThrow(() -> invalid("Không tìm thấy chi tiết đơn hàng hợp lệ"));
        if (!Objects.equals(detail.getOrder().getUser().getId(), user.getId())
            || !Objects.equals(detail.getProduct().getId(), productId)
            || detail.getOrder().getStatus() != OrderStatus.COMPLETED)
            throw invalid("Bạn chỉ có thể đánh giá sản phẩm trong đơn hàng đã hoàn tất của mình");
        if (reviews.existsByOrderDetailId(detail.getId()))
            throw invalid("Chi tiết đơn hàng này đã được đánh giá, không thể gửi thêm");
        Review review = new Review();
        review.setUser(user); review.setProduct(detail.getProduct()); review.setOrderDetail(detail);
        review.setRating(request.getRating());
        review.setComment(request.getComment() == null || request.getComment().isBlank() ? null : request.getComment().trim());
        review.setStatus(ReviewStatus.PENDING);
        for (String key : storage.store(request.getImages())) {
            ReviewImage image = new ReviewImage(); image.setStorageKey(key); review.addImage(image);
        }
        return reviews.saveAndFlush(review);
    }

    @Transactional(readOnly = true)
    public Page<Review> searchAdmin(String keyword, ReviewStatus status, int page) {
        return reviews.searchAdmin(keyword == null ? "" : keyword.trim(), status,
            PageRequest.of(Math.max(0, page), 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional(readOnly = true)
    public Review adminDetail(Long id) {
        return reviews.findAdminDetail(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy đánh giá"));
    }

    @Transactional
    public void moderate(Long id, ReviewStatus target, String note, String adminEmail) {
        User admin = users.findByEmailIgnoreCase(adminEmail).filter(User::isStatus)
            .filter(u -> u.getRole() == Role.ADMIN).orElseThrow(() -> invalid("Không có quyền duyệt đánh giá"));
        if (target == null || target == ReviewStatus.PENDING) throw invalid("Thao tác không hợp lệ");
        if (note != null && note.length() > 500) throw invalid("Lý do tối đa 500 ký tự");
        if ((target == ReviewStatus.HIDDEN || target == ReviewStatus.DELETED) && (note == null || note.isBlank()))
            throw invalid("Vui lòng nhập lý do ẩn hoặc xóa");
        Review review = reviews.lockById(id).orElseThrow(() -> invalid("Không tìm thấy đánh giá"));
        if (review.getStatus() == ReviewStatus.DELETED) throw invalid("Đánh giá đã xóa không được thay đổi");
        if (target == ReviewStatus.APPROVED && !review.isVerifiedPurchase())
            throw invalid("Đánh giá cũ chưa xác minh đơn hàng nên không thể duyệt");
        review.setStatus(target); review.setModerationNote(note == null ? null : note.trim());
        review.setModeratedBy(admin); review.setModeratedAt(java.time.LocalDateTime.now());
        // Soft deletion preserves the purchase uniqueness and moderation trace.
        reviews.save(review);
    }

    private ReviewOperationException invalid(String message) { return new ReviewOperationException(message); }
}
