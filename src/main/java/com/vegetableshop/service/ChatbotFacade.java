package com.vegetableshop.service;

import com.vegetableshop.dto.AiChatDecision;
import com.vegetableshop.dto.ChatbotProductView;
import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("mysql")
public class ChatbotFacade {

    private static final Logger log = LoggerFactory.getLogger(ChatbotFacade.class);
    private static final int CANDIDATE_LIMIT = 60;
    private static final int PRODUCT_LIMIT = 5;
    private static final int SUGGESTION_LIMIT = 4;

    private final ChatbotService fallbackService;
    private final ProductRepository productRepository;
    private final ObjectProvider<AiChatClient> aiChatClientProvider;
    private final ChatbotFaqService faqService;
    private final ChatbotAnalyticsService analyticsService;

    public ChatbotFacade(
        ChatbotService fallbackService,
        ProductRepository productRepository,
        ObjectProvider<AiChatClient> aiChatClientProvider,
        ChatbotFaqService faqService,
        ChatbotAnalyticsService analyticsService
    ) {
        this.fallbackService = fallbackService;
        this.productRepository = productRepository;
        this.aiChatClientProvider = aiChatClientProvider;
        this.faqService = faqService;
        this.analyticsService = analyticsService;
    }

    public ChatbotResponse reply(String question) {
        long startedAt = System.nanoTime();
        try {
            var managedFaq = faqService.findMatch(question);
            if (managedFaq.isPresent()) {
                var match = managedFaq.get();
                return finish(question, match.response(), ChatbotResponseSource.MANAGED_FAQ,
                    ChatbotInteractionStatus.RESOLVED, startedAt, match.faqId());
            }
        } catch (RuntimeException exception) {
            log.warn("Managed chatbot FAQ unavailable; continuing without it ({})",
                exception.getClass().getSimpleName());
        }

        AiChatClient aiClient = aiChatClientProvider.getIfAvailable();
        if (aiClient == null) {
            ChatbotResponse fallback = fallbackService.reply(question);
            return finish(question, fallback, ChatbotResponseSource.RULE_BASED,
                fallbackStatus(fallback), startedAt, null);
        }

        try {
            List<Product> candidates = productRepository.findChatbotCandidates(
                0, PageRequest.of(0, CANDIDATE_LIMIT)
            );
            AiChatDecision decision = aiClient.decide(question, candidates);
            ChatbotResponse response = validatedAiResponse(decision, candidates);
            return finish(question, response, ChatbotResponseSource.AI,
                ChatbotInteractionStatus.RESOLVED, startedAt, null);
        } catch (RuntimeException exception) {
            log.warn("AI chatbot unavailable; using the local fallback ({})",
                exception.getClass().getSimpleName());
            ChatbotResponse fallback = fallbackService.reply(question);
            return finish(question, fallback, ChatbotResponseSource.AI_FALLBACK,
                fallbackStatus(fallback), startedAt, null);
        }
    }

    private ChatbotResponse finish(
        String question,
        ChatbotResponse response,
        ChatbotResponseSource source,
        ChatbotInteractionStatus status,
        long startedAt,
        Long faqId
    ) {
        long elapsedMs = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        try {
            analyticsService.record(question, response, source, status, elapsedMs, faqId);
        } catch (RuntimeException exception) {
            log.warn("Could not persist chatbot analytics ({})",
                exception.getClass().getSimpleName());
        }
        return response;
    }

    private ChatbotInteractionStatus fallbackStatus(ChatbotResponse response) {
        String message = response == null || response.message() == null
            ? "" : response.message().toLowerCase(java.util.Locale.ROOT);
        return message.contains("chưa hiểu") || message.contains("chưa tìm thấy")
            ? ChatbotInteractionStatus.NEEDS_REVIEW
            : ChatbotInteractionStatus.RESOLVED;
    }

    private ChatbotResponse validatedAiResponse(AiChatDecision decision, List<Product> candidates) {
        if (decision == null || decision.answer() == null || decision.answer().isBlank()) {
            throw new IllegalStateException("AI response did not contain an answer");
        }

        Map<Long, Product> byId = candidates.stream().collect(Collectors.toMap(
            Product::getId,
            Function.identity(),
            (left, right) -> left
        ));
        LinkedHashSet<Long> requestedIds = new LinkedHashSet<>(safeList(decision.productIds()));
        List<ChatbotProductView> products = requestedIds.stream()
            .limit(PRODUCT_LIMIT)
            .map(byId::get)
            .filter(product -> product != null && product.isStatus() && product.getQuantity() > 0)
            .map(fallbackService::toView)
            .toList();

        List<String> suggestions = safeList(decision.suggestions()).stream()
            .map(String::trim)
            .filter(value -> !value.isBlank() && value.length() <= 80)
            .distinct()
            .limit(SUGGESTION_LIMIT)
            .toList();
        return ChatbotResponse.ai(decision.answer().trim(), products, suggestions);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? new ArrayList<>() : values;
    }
}
