package com.vegetableshop.service;

import com.vegetableshop.dto.AdminChatbotFaqRequest;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.repository.ChatbotFaqRepository;
import com.vegetableshop.repository.ChatbotInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminChatbotServiceTests {

    @Mock private ChatbotFaqRepository faqRepository;
    @Mock private ChatbotInteractionRepository interactionRepository;

    @Test
    void createsNormalizedFaqWithAdminAudit() {
        AdminChatbotFaqRequest request = request();
        when(faqRepository.findByQuestionIgnoreCase("Phí giao hàng?")).thenReturn(Optional.empty());
        when(faqRepository.save(any(ChatbotFaq.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ChatbotFaq saved = service().createFaq(request, "admin@example.com");

        assertEquals("Phí giao hàng?", saved.getQuestion());
        assertEquals("phí ship, vận chuyển", saved.getKeywords());
        assertEquals("admin@example.com", saved.getCreatedBy());
        assertEquals("admin@example.com", saved.getUpdatedBy());
        assertTrue(saved.isStatus());
    }

    @Test
    void dashboardAggregatesSourcesAndNeedsReview() {
        LocalDate today = LocalDate.now();
        List<ChatbotInteraction> interactions = List.of(
            interaction(today.atTime(8, 0), ChatbotResponseSource.MANAGED_FAQ,
                ChatbotInteractionStatus.RESOLVED, 10),
            interaction(today.atTime(9, 0), ChatbotResponseSource.AI,
                ChatbotInteractionStatus.RESOLVED, 30),
            interaction(today.atTime(10, 0), ChatbotResponseSource.AI_FALLBACK,
                ChatbotInteractionStatus.NEEDS_REVIEW, 20)
        );
        when(interactionRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
            .thenReturn(interactions);
        when(interactionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(Page.empty());
        when(faqRepository.countByStatusTrue()).thenReturn(5L);

        var dashboard = service().dashboard(today, today, "", null, null, 0);

        assertEquals(3, dashboard.totalInteractions());
        assertEquals(1, dashboard.faqInteractions());
        assertEquals(1, dashboard.aiInteractions());
        assertEquals(1, dashboard.fallbackInteractions());
        assertEquals(1, dashboard.needsReview());
        assertEquals(20, dashboard.averageResponseTimeMs());
        assertEquals(5, dashboard.activeFaqs());
    }

    @Test
    void purgesInteractionsUsingConfiguredRetention() {
        when(interactionRepository.deleteByCreatedAtBefore(any())).thenReturn(4L);

        assertEquals(4, service().purgeExpiredInteractions());
        verify(interactionRepository).deleteByCreatedAtBefore(any());
    }

    private AdminChatbotService service() {
        return new AdminChatbotService(faqRepository, interactionRepository, 30);
    }

    private AdminChatbotFaqRequest request() {
        AdminChatbotFaqRequest request = new AdminChatbotFaqRequest();
        request.setQuestion("  Phí   giao hàng?  ");
        request.setAnswer(" Trả lời chính thức ");
        request.setKeywords(" phí ship ; vận chuyển ");
        request.setDisplayOrder(10);
        request.setStatus(true);
        return request;
    }

    private ChatbotInteraction interaction(
        LocalDateTime createdAt,
        ChatbotResponseSource source,
        ChatbotInteractionStatus status,
        long elapsed
    ) {
        ChatbotInteraction item = new ChatbotInteraction();
        item.setCreatedAt(createdAt);
        item.setResponseSource(source);
        item.setStatus(status);
        item.setResponseTimeMs(elapsed);
        return item;
    }
}
