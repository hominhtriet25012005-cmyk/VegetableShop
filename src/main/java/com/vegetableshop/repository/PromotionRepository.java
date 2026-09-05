package com.vegetableshop.repository;

import com.vegetableshop.entity.Promotion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @EntityGraph(attributePaths = {"products", "products.product"})
    List<Promotion> findByStatusTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
        LocalDateTime startsAt, LocalDateTime endsAt);
    @EntityGraph(attributePaths = {"products", "products.product"})
    List<Promotion> findAllByOrderByCreatedAtDesc();

    @Query("""
        select distinct promotion
        from Promotion promotion
        left join fetch promotion.products promotionProduct
        left join fetch promotionProduct.product
        where promotion.id = :id
        """)
    Optional<Promotion> findDetailedById(@Param("id") Long id);
}
