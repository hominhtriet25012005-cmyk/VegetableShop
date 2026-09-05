package com.vegetableshop.repository;

import com.vegetableshop.entity.StockMovement;
import com.vegetableshop.entity.StockMovementType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class StockMovementSpecifications {

    private StockMovementSpecifications() {
    }

    public static Specification<StockMovement> withFilters(
        Long productId,
        StockMovementType movementType,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (productId != null && productId > 0) {
                predicates.add(builder.equal(root.get("product").get("id"), productId));
            }
            if (movementType != null) {
                predicates.add(builder.equal(root.get("movementType"), movementType));
            }
            if (fromDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
