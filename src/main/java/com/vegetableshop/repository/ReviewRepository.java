package com.vegetableshop.repository;

import com.vegetableshop.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<Review> findByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    @Query("select avg(r.rating), count(r) from Review r where r.product.id = :productId")
    Object[] summarize(@Param("productId") Long productId);
}
