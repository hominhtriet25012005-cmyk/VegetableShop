package com.vegetableshop.controller;

import com.vegetableshop.entity.ReviewStatus;
import com.vegetableshop.repository.ReviewImageRepository;
import com.vegetableshop.service.ReviewImageStorage;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@Profile("mysql")
public class ReviewImageController {
    private final ReviewImageRepository images;
    private final ReviewImageStorage storage;
    public ReviewImageController(ReviewImageRepository images, ReviewImageStorage storage) {
        this.images = images; this.storage = storage;
    }
    @GetMapping("/review-images/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> image(@PathVariable Long id, Authentication authentication) {
        var image = images.findOneById(id);
        if (image.isEmpty()) return ResponseEntity.notFound().build();
        var review = image.get().getReview();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean owner = authentication != null && authentication.isAuthenticated()
            && review.getUser().isStatus() && review.getUser().getEmail().equalsIgnoreCase(authentication.getName());
        boolean published = review.getStatus() == ReviewStatus.APPROVED && review.isVerifiedPurchase();
        if (!admin && !published && !(owner && review.getStatus() != ReviewStatus.DELETED))
            return ResponseEntity.notFound().build();
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(storage.read(image.get().getStorageKey()));
        } catch (IOException exception) { return ResponseEntity.notFound().build(); }
    }
}
