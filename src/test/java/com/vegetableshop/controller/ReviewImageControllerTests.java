package com.vegetableshop.controller;

import com.vegetableshop.entity.*;
import com.vegetableshop.repository.ReviewImageRepository;
import com.vegetableshop.service.ReviewImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewImageControllerTests {
    @Test void unpublishedImagesOnlyVisibleToOwnerOrAdminAndDeletedImagesOnlyToAdmin() throws Exception {
        var repo = mock(ReviewImageRepository.class); var storage = mock(ReviewImageStorage.class);
        var user = new User(); user.setEmail("owner@example.com");
        var review = new Review(); review.setUser(user); review.setOrderDetail(new OrderDetail());
        var image = new ReviewImage(); image.setReview(review); image.setStorageKey("photo.jpg");
        when(repo.findOneById(1L)).thenReturn(Optional.of(image));
        when(storage.read("photo.jpg")).thenReturn(new byte[]{1,2,3});
        var controller = new ReviewImageController(repo, storage);
        var owner = UsernamePasswordAuthenticationToken.authenticated("owner@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var other = UsernamePasswordAuthenticationToken.authenticated("other@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var admin = UsernamePasswordAuthenticationToken.authenticated("admin@example.com", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertEquals(404, controller.image(1L, null).getStatusCode().value());
        assertEquals(404, controller.image(1L, other).getStatusCode().value());
        assertEquals(200, controller.image(1L, owner).getStatusCode().value());
        assertEquals(200, controller.image(1L, admin).getStatusCode().value());
        review.setStatus(ReviewStatus.APPROVED);
        var published = controller.image(1L, null);
        assertEquals(200, published.getStatusCode().value());
        assertEquals("no-store", published.getHeaders().getCacheControl());
        review.setStatus(ReviewStatus.HIDDEN);
        assertEquals(404, controller.image(1L, null).getStatusCode().value());
        review.setStatus(ReviewStatus.DELETED);
        assertEquals(404, controller.image(1L, owner).getStatusCode().value());
        assertEquals(200, controller.image(1L, admin).getStatusCode().value());
    }
}
