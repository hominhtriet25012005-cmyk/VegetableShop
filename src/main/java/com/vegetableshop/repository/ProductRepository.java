package com.vegetableshop.repository;

import com.vegetableshop.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findByStatusTrueOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"category", "supplier"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findByIdAndStatusTrueAndCategoryStatusTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from Product p
        join fetch p.category c
        where p.id = :id and p.status = true and c.status = true
        """)
    Optional<Product> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findTop3ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findTop8ByStatusTrueAndCategoryStatusTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findTop8ByStatusTrueAndCategoryStatusTrueAndCategoryIdOrderByCreatedAtDesc(Long categoryId);

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findTop4ByStatusTrueAndCategoryStatusTrueAndCategoryIdAndIdNotOrderByCreatedAtDesc(
        Long categoryId,
        Long productId
    );

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findTop4ByStatusTrueAndCategoryStatusTrueAndSupplierIdAndIdNotOrderByCreatedAtDesc(
        Long supplierId,
        Long productId
    );

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findByIdInAndStatusTrueAndCategoryStatusTrue(List<Long> ids);

    @EntityGraph(attributePaths = "category")
    List<Product> findTop5ByStatusTrueAndQuantityLessThanEqualOrderByQuantityAsc(Integer quantity);
}
