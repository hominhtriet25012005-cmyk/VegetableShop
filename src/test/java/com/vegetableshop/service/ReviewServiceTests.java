package com.vegetableshop.service;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.entity.*;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTests {
    @Mock ReviewRepository reviews;
    @Mock UserRepository users;
    @Mock OrderDetailRepository details;
    @Mock ReviewImageStorage storage;

    private ReviewService service() { return new ReviewService(reviews, users, details, storage); }
    private ReviewRequest request() {
        var r = new ReviewRequest(); r.setOrderDetailId(10L); r.setRating(5); r.setComment("  Rất tươi  "); return r;
    }
    private OrderDetail purchase() {
        var user = new User(); user.setId(7L); user.setEmail("user@example.com");
        var product = new Product(); product.setId(1L);
        var order = new Order(); order.setUser(user); order.setStatus(OrderStatus.COMPLETED);
        var detail = new OrderDetail(); detail.setId(10L); detail.setProduct(product); detail.setOrder(order);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(details.lockForReview(10L)).thenReturn(Optional.of(detail));
        return detail;
    }
    @Test void completedPurchaseCreatesPendingReviewWithImages() {
        var detail = purchase();
        when(storage.store(any())).thenReturn(List.of("photo.jpg"));
        when(reviews.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = service().save("user@example.com", 1L, request());
        assertEquals(detail, result.getOrderDetail());
        assertEquals("Rất tươi", result.getComment());
        assertEquals(ReviewStatus.PENDING, result.getStatus());
        assertTrue(result.isVerifiedPurchase());
        assertEquals("photo.jpg", result.getImages().getFirst().getStorageKey());
        assertSame(result, result.getImages().getFirst().getReview());
    }
    @Test void nonCompletedOrderCannotReview() {
        purchase().getOrder().setStatus(OrderStatus.SHIPPING);
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 1L, request()));
        verifyNoInteractions(storage);
    }
    @Test void wrongProductCannotReview() {
        purchase();
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 2L, request()));
        verifyNoInteractions(storage);
    }
    @Test void someoneElsesOrderCannotReview() {
        var detail = purchase();
        User other = new User(); other.setId(999L); detail.getOrder().setUser(other);
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 1L, request()));
        verifyNoInteractions(storage);
    }
    @Test void duplicateEvenIfHiddenOrDeletedCannotBeSubmittedAgain() {
        purchase(); when(reviews.existsByOrderDetailId(10L)).thenReturn(true);
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 1L, request()));
        verifyNoInteractions(storage);
    }
    @Test void invalidRatingAndMissingPurchaseRejectedAtServiceBoundary() {
        var request = request(); request.setRating(0);
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 1L, request));
        request.setRating(5); request.setOrderDetailId(null);
        assertThrows(ReviewOperationException.class, () -> service().save("user@example.com", 1L, request));
        verifyNoInteractions(details, storage);
    }
    @Test void anonymousHasNoPrivateReviewData() {
        assertTrue(service().eligiblePurchases(null, 1L).isEmpty());
        assertTrue(service().findOwnReviews(null, 1L).isEmpty());
        verifyNoInteractions(reviews, details);
    }
    @Test void summaryCalculatesDistributionIncludingZeroStarBuckets() {
        when(reviews.countPublishedByRating(1L)).thenReturn(List.of(new Object[]{5, 3L}, new Object[]{1, 1L}));
        var summary = service().summarize(1L);
        assertEquals(4.0, summary.averageRating()); assertEquals(4L, summary.reviewCount());
        assertEquals(75, summary.percentFor(5)); assertEquals(0, summary.countFor(2));
        when(reviews.countPublishedByRating(2L)).thenReturn(List.of());
        assertEquals(0, service().summarize(2L).percentFor(5));
    }
    private Review moderationFixture() {
        var admin = new User(); admin.setRole(Role.ADMIN);
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        var r = new Review(); r.setOrderDetail(new OrderDetail());
        when(reviews.lockById(1L)).thenReturn(Optional.of(r)); return r;
    }
    @Test void moderationRecordsActorNoteAndTime() {
        var r = moderationFixture();
        service().moderate(1L, ReviewStatus.APPROVED, "  Hợp lệ  ", "admin@example.com");
        assertEquals(ReviewStatus.APPROVED, r.getStatus()); assertEquals("Hợp lệ", r.getModerationNote());
        assertNotNull(r.getModeratedAt()); assertNotNull(r.getModeratedBy());
        service().moderate(1L, ReviewStatus.HIDDEN, "Nội dung vi phạm", "admin@example.com");
        assertEquals(ReviewStatus.HIDDEN, r.getStatus());
    }
    @Test void deletedReviewCannotBeRestoredOrResubmitted() {
        var r = moderationFixture();
        service().moderate(1L, ReviewStatus.DELETED, "Vi phạm", "admin@example.com");
        assertEquals(ReviewStatus.DELETED, r.getStatus());
        assertThrows(ReviewOperationException.class, () -> service().moderate(1L, ReviewStatus.APPROVED, "", "admin@example.com"));
    }
    @Test void legacyUnverifiedCannotBeApproved() {
        moderationFixture().setOrderDetail(null);
        assertThrows(ReviewOperationException.class, () -> service().moderate(1L, ReviewStatus.APPROVED, "", "admin@example.com"));
    }
    @Test void customerCannotModerate() {
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(new User()));
        assertThrows(ReviewOperationException.class, () -> service().moderate(1L, ReviewStatus.APPROVED, "", "user@example.com"));
        verify(reviews, never()).lockById(any());
    }
    @Test void hidingRequiresReason() {
        var admin = new User(); admin.setRole(Role.ADMIN);
        when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        assertThrows(ReviewOperationException.class, () -> service().moderate(1L, ReviewStatus.HIDDEN, " ", "admin@example.com"));
        verify(reviews, never()).lockById(any());
    }
}
