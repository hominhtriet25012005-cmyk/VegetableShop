package com.vegetableshop.service;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.repository.ChatbotFaqRepository;
import com.vegetableshop.repository.ChatbotInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotAnalyticsServiceTests {

    @Mock private ChatbotInteractionRepository interactionRepository;
    @Mock private ChatbotFaqRepository faqRepository;

    @Test
    void persistsOnlyRedactedQuestionAndOperationalMetadata() {
        ChatbotFaq faq = new ChatbotFaq();
        faq.setId(9L);
        when(faqRepository.findById(9L)).thenReturn(Optional.of(faq));
        ChatbotResponse response = ChatbotResponse.faq("Nội dung trả lời", List.of());

        service().record(
            "Email toi@example.com, gọi 0901 234 567, mã 1234567890123456",
            response, ChatbotResponseSource.MANAGED_FAQ,
            ChatbotInteractionStatus.RESOLVED, -5, 9L
        );

        ArgumentCaptor<ChatbotInteraction> captor = ArgumentCaptor.forClass(ChatbotInteraction.class);
        verify(interactionRepository).save(captor.capture());
        ChatbotInteraction saved = captor.getValue();
        assertEquals("Email [email], gọi [số điện thoại], mã [dãy số nhạy cảm]",
            saved.getQuestion());
        assertFalse(saved.getQuestion().contains("toi@example.com"));
        assertEquals(ChatbotResponseSource.MANAGED_FAQ, saved.getResponseSource());
        assertEquals(0L, saved.getResponseTimeMs());
        assertEquals(faq, saved.getMatchedFaq());
    }

    private ChatbotAnalyticsService service() {
        return new ChatbotAnalyticsService(interactionRepository, faqRepository);
    }
}
