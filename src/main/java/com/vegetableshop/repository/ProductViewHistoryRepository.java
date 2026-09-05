package com.vegetableshop.repository;

import com.vegetableshop.entity.ProductViewHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductViewHistoryRepository extends JpaRepository<ProductViewHistory, Long> {

    Optional<ProductViewHistory> findByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.supplier"})
    List<ProductViewHistory> findTop6ByUserEmailIgnoreCaseAndProductIdNotOrderByUpdatedAtDesc(
        String email,
        Long productId
    );
}
