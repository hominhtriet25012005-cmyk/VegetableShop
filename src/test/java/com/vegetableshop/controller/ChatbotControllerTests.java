package com.vegetableshop.controller;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.service.ChatbotFacade;
import com.vegetableshop.service.ChatbotRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerTests {

    @Mock
    private ChatbotFacade chatbotFacade;

    @Mock
    private ChatbotRateLimiter rateLimiter;

    @Test
    void rejectsBlankOrOversizedQuestions() {
        ChatbotController controller = new ChatbotController(chatbotFacade, rateLimiter);

        ResponseEntity<ChatbotResponse> blank = controller.reply("   ", new MockHttpSession());
        ResponseEntity<ChatbotResponse> oversized = controller.reply("x".repeat(301), new MockHttpSession());

        assertEquals(HttpStatus.BAD_REQUEST, blank.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, oversized.getStatusCode());
        verifyNoInteractions(chatbotFacade, rateLimiter);
    }

    @Test
    void returnsRateLimitResponseBeforeCallingBusinessService() {
        MockHttpSession session = new MockHttpSession();
        when(rateLimiter.tryAcquire(session)).thenReturn(false);
        ChatbotController controller = new ChatbotController(chatbotFacade, rateLimiter);

        ResponseEntity<ChatbotResponse> response = controller.reply("Tìm rau củ", session);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        verifyNoInteractions(chatbotFacade);
    }

    @Test
    void trimsValidQuestionAndReturnsServiceResponse() {
        MockHttpSession session = new MockHttpSession();
        ChatbotResponse expected = ChatbotResponse.message("Kết quả", List.of("Tìm tiếp"));
        when(rateLimiter.tryAcquire(session)).thenReturn(true);
        when(chatbotFacade.reply("Tìm nấm")).thenReturn(expected);
        ChatbotController controller = new ChatbotController(chatbotFacade, rateLimiter);

        ResponseEntity<ChatbotResponse> response = controller.reply("  Tìm nấm  ", session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(expected, response.getBody());
        verify(chatbotFacade).reply("Tìm nấm");
    }
}
