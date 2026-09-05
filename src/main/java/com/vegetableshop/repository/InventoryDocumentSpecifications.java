package com.vegetableshop.repository;

import com.vegetableshop.entity.InventoryDocument;
import com.vegetableshop.entity.InventoryDocumentStatus;
import com.vegetableshop.entity.InventoryDocumentType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class InventoryDocumentSpecifications {

    private InventoryDocumentSpecifications() {
    }

    public static Specification<InventoryDocument> withFilters(
        String keyword,
        InventoryDocumentType type,
        InventoryDocumentStatus status,
        Long supplierId,
        Long productId,
        LocalDate from,
        LocalDate to
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("code")), pattern),
                    builder.like(builder.lower(root.get("invoiceReference")), pattern)
                ));
            }
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (supplierId != null) predicates.add(builder.equal(root.get("supplier").get("id"), supplierId));
            if (productId != null) {
                predicates.add(builder.equal(root.join("items").get("product").get("id"), productId));
                query.distinct(true);
            }
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
