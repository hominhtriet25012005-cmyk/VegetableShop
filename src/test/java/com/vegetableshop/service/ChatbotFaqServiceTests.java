package com.vegetableshop.service;

import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.repository.ChatbotFaqRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotFaqServiceTests {

    @Mock private ChatbotFaqRepository faqRepository;

    @Test
    void matchesVietnameseQuestionWithoutDependingOnAccents() {
        ChatbotFaq shipping = faq(7L, "Phí giao hàng được tính như thế nào?",
            "Phí được hiển thị khi thanh toán.", "phí giao hàng, phí ship", 10);
        when(faqRepository.findByStatusTrueOrderByDisplayOrderAscIdAsc())
            .thenReturn(List.of(shipping));

        var match = service().findMatch("PHI SHIP của cửa hàng là bao nhiêu?");

        assertTrue(match.isPresent());
        assertEquals(7L, match.orElseThrow().faqId());
        assertEquals("FAQ", match.orElseThrow().response().mode());
        assertEquals("Phí được hiển thị khi thanh toán.",
            match.orElseThrow().response().message());
    }

    @Test
    void returnsEmptyWhenNoManagedAnswerMatches() {
        when(faqRepository.findByStatusTrueOrderByDisplayOrderAscIdAsc())
            .thenReturn(List.of(faq(1L, "Phí giao hàng", "Trả lời", "phí ship", 1)));

        assertTrue(service().findMatch("Tìm nấm dưới 100 nghìn").isEmpty());
    }

    private ChatbotFaqService service() {
        return new ChatbotFaqService(faqRepository);
    }

    private ChatbotFaq faq(Long id, String question, String answer, String keywords, int order) {
        ChatbotFaq faq = new ChatbotFaq();
        faq.setId(id);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setKeywords(keywords);
        faq.setDisplayOrder(order);
        faq.setStatus(true);
        return faq;
    }
}
