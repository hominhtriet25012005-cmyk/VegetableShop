package com.vegetableshop.repository;

import com.vegetableshop.entity.ReviewImage;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    @EntityGraph(attributePaths = {"review", "review.user", "review.orderDetail"})
    Optional<ReviewImage> findOneById(Long id);
}
