package com.vegetableshop.repository;

import com.vegetableshop.dto.ProductFilter;
import com.vegetableshop.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the optional search predicates used by the Shop page.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(ProductFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("status")));
            predicates.add(criteriaBuilder.isTrue(root.get("category").get("status")));

            if (filter.getKeyword() != null) {
                String pattern = "%" + filter.getKeyword().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
            }
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(
                    root.get("category").get("id"), filter.getCategoryId()
                ));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("price"), filter.getMinPrice()
                ));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("price"), filter.getMaxPrice()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
