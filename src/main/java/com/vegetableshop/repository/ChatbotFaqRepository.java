package com.vegetableshop.repository;

import com.vegetableshop.entity.ChatbotFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatbotFaqRepository extends JpaRepository<ChatbotFaq, Long> {
    List<ChatbotFaq> findByStatusTrueOrderByDisplayOrderAscIdAsc();
    List<ChatbotFaq> findAllByOrderByDisplayOrderAscIdAsc();
    Optional<ChatbotFaq> findByQuestionIgnoreCase(String question);
    long countByStatusTrue();
}
