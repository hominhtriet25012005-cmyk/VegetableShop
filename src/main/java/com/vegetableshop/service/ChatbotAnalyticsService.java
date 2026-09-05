package com.vegetableshop.service;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.repository.ChatbotFaqRepository;
import com.vegetableshop.repository.ChatbotInteractionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@Profile("mysql")
public class ChatbotAnalyticsService {

    private static final Pattern EMAIL = Pattern.compile(
        "(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}"
    );
    private static final Pattern PHONE = Pattern.compile(
        "(?<!\\d)(?:\\+?84|0)(?:[ .-]?\\d){9,10}(?!\\d)"
    );
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d{12,19}(?!\\d)");

    private final ChatbotInteractionRepository interactionRepository;
    private final ChatbotFaqRepository faqRepository;

    public ChatbotAnalyticsService(
        ChatbotInteractionRepository interactionRepository,
        ChatbotFaqRepository faqRepository
    ) {
        this.interactionRepository = interactionRepository;
        this.faqRepository = faqRepository;
    }

    @Transactional
    public void record(
        String question,
        ChatbotResponse response,
        ChatbotResponseSource source,
        ChatbotInteractionStatus status,
        long responseTimeMs,
        Long faqId
    ) {
        ChatbotInteraction interaction = new ChatbotInteraction();
        interaction.setQuestion(redact(question));
        interaction.setResponseSource(source);
        interaction.setStatus(status);
        interaction.setProductCount(response == null || response.products() == null
            ? 0 : response.products().size());
        interaction.setResponseTimeMs(Math.max(0, responseTimeMs));
        if (faqId != null) {
            ChatbotFaq faq = faqRepository.findById(faqId).orElse(null);
            interaction.setMatchedFaq(faq);
        }
        interactionRepository.save(interaction);
    }

    String redact(String value) {
        String safe = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        safe = EMAIL.matcher(safe).replaceAll("[email]");
        safe = PHONE.matcher(safe).replaceAll("[số điện thoại]");
        safe = LONG_NUMBER.matcher(safe).replaceAll("[dãy số nhạy cảm]");
        safe = safe.replaceAll("\\s{2,}", " ");
        return safe.length() <= 300 ? safe : safe.substring(0, 300);
    }
}
