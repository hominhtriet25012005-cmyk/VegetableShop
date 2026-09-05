package com.vegetableshop.repository;

import com.vegetableshop.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.supplier"})
    List<Wishlist> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    Optional<Wishlist> findByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    boolean existsByUserEmailIgnoreCaseAndProductId(String email, Long productId);

    @Query("""
        select count(w) from Wishlist w
        where lower(w.user.email) = lower(:email)
          and w.product.status = true
          and w.product.category.status = true
        """)
    long countActiveByUserEmail(@Param("email") String email);

    @Query("""
        select w.product.id from Wishlist w
        where lower(w.user.email) = lower(:email)
          and w.product.status = true
          and w.product.category.status = true
        """)
    List<Long> findActiveProductIdsByUserEmail(@Param("email") String email);
}
