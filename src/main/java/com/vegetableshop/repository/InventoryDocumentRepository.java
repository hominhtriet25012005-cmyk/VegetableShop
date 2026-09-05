package com.vegetableshop.repository;

import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryDocumentRepository
    extends JpaRepository<InventoryDocument, Long>, JpaSpecificationExecutor<InventoryDocument> {

    @Override
    @EntityGraph(attributePaths = {"supplier"})
    Page<InventoryDocument> findAll(org.springframework.data.jpa.domain.Specification<InventoryDocument> spec,
                                    Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    @Query("select distinct d from InventoryDocument d where d.id = :id")
    Optional<InventoryDocument> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from InventoryDocument d where d.id = :id")
    Optional<InventoryDocument> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "items"})
    List<InventoryDocument> findByTypeAndStatusAndPostedAtGreaterThanEqualAndPostedAtLessThan(
        InventoryDocumentType type,
        InventoryDocumentStatus status,
        LocalDateTime from,
        LocalDateTime to
    );
}
