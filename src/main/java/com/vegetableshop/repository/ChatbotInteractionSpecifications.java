package com.vegetableshop.repository;

import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class ChatbotInteractionSpecifications {

    private ChatbotInteractionSpecifications() {
    }

    public static Specification<ChatbotInteraction> filter(
        String keyword,
        ChatbotResponseSource source,
        ChatbotInteractionStatus status,
        LocalDateTime from,
        LocalDateTime to
    ) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.between(root.get("createdAt"), from, to));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(builder.like(
                    builder.lower(root.get("question")),
                    "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%"
                ));
            }
            if (source != null) predicates.add(builder.equal(root.get("responseSource"), source));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
