package com.vegetableshop.controller;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.service.ChatbotFacade;
import com.vegetableshop.service.ChatbotRateLimiter;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Profile("mysql")
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final int MAX_MESSAGE_LENGTH = 300;

    private final ChatbotFacade chatbotFacade;
    private final ChatbotRateLimiter rateLimiter;

    public ChatbotController(ChatbotFacade chatbotFacade, ChatbotRateLimiter rateLimiter) {
        this.chatbotFacade = chatbotFacade;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/messages")
    public ResponseEntity<ChatbotResponse> reply(
        @RequestParam(required = false) String message,
        HttpSession session
    ) {
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            return ResponseEntity.badRequest().body(ChatbotResponse.message(
                "Câu hỏi phải có từ 1 đến 300 ký tự.", List.of()
            ));
        }
        if (!rateLimiter.tryAcquire(session)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ChatbotResponse.message(
                "Bạn gửi câu hỏi quá nhanh. Vui lòng thử lại sau khoảng một phút.", List.of()
            ));
        }
        return ResponseEntity.ok(chatbotFacade.reply(message.trim()));
    }
}
