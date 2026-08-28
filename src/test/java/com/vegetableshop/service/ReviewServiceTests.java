package com.vegetableshop.service;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.Review;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.ReviewOperationException;
import com.vegetableshop.repository.OrderRepository;
import com.vegetableshop.repository.ReviewRepository;
import com.vegetableshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTests {

    @Mock ReviewRepository reviewRepository;
    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductService productService;

    @Test
    void customerCannotReviewBeforeCompletedPurchase() {
        when(orderRepository.hasCompletedPurchase("user@example.com", 1L)).thenReturn(false);

        assertThrows(ReviewOperationException.class,
            () -> service().save("user@example.com", 1L, request(5, "Tốt")));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void verifiedCustomerCanCreateReviewAndCommentIsTrimmed() {
        User user = new User();
        user.setStatus(true);
        Product product = new Product();
        when(orderRepository.hasCompletedPurchase("user@example.com", 1L)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(productService.findActiveProduct(1L)).thenReturn(product);
        when(reviewRepository.findByUserEmailIgnoreCaseAndProductId("user@example.com", 1L))
            .thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review saved = service().save("user@example.com", 1L, request(5, "  Rất tươi  "));

        assertEquals(5, saved.getRating());
        assertEquals("Rất tươi", saved.getComment());
        assertEquals(user, saved.getUser());
        assertEquals(product, saved.getProduct());
    }

    private ReviewService service() {
        return new ReviewService(reviewRepository, userRepository, orderRepository, productService);
    }

    private ReviewRequest request(int rating, String comment) {
        ReviewRequest request = new ReviewRequest();
        request.setRating(rating);
        request.setComment(comment);
        return request;
    }
}
