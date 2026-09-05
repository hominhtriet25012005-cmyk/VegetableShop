package com.vegetableshop.repository;

import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatbotInteractionRepository extends
    JpaRepository<ChatbotInteraction, Long>, JpaSpecificationExecutor<ChatbotInteraction> {

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByResponseSourceAndCreatedAtBetween(
        ChatbotResponseSource source, LocalDateTime from, LocalDateTime to
    );
    long countByStatusAndCreatedAtBetween(
        ChatbotInteractionStatus status, LocalDateTime from, LocalDateTime to
    );
    List<ChatbotInteraction> findByCreatedAtBetweenOrderByCreatedAtAsc(
        LocalDateTime from, LocalDateTime to
    );
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
