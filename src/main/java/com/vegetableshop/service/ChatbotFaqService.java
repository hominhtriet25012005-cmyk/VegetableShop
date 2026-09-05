package com.vegetableshop.service;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.repository.ChatbotFaqRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Profile("mysql")
public class ChatbotFaqService {

    private static final List<String> SUGGESTIONS = List.of(
        "Tìm sản phẩm", "Phí giao hàng", "Thanh toán thế nào?", "Liên hệ cửa hàng"
    );

    private final ChatbotFaqRepository faqRepository;

    public ChatbotFaqService(ChatbotFaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public Optional<FaqMatch> findMatch(String rawQuestion) {
        String question = normalize(rawQuestion);
        if (question.isBlank()) return Optional.empty();
        return faqRepository.findByStatusTrueOrderByDisplayOrderAscIdAsc().stream()
            .map(faq -> new ScoredFaq(faq, score(faq, question)))
            .filter(candidate -> candidate.score() > 0)
            .max(Comparator.comparingInt(ScoredFaq::score)
                .thenComparing(candidate -> -candidate.faq().getDisplayOrder()))
            .map(candidate -> new FaqMatch(
                candidate.faq().getId(),
                ChatbotResponse.faq(candidate.faq().getAnswer(), SUGGESTIONS)
            ));
    }

    private int score(ChatbotFaq faq, String normalizedQuestion) {
        String canonicalQuestion = normalize(faq.getQuestion());
        if (normalizedQuestion.equals(canonicalQuestion)) return 1000;
        int score = canonicalQuestion.length() >= 8 && normalizedQuestion.contains(canonicalQuestion)
            ? 200 : 0;
        for (String keyword : splitKeywords(faq.getKeywords())) {
            String phrase = normalize(keyword);
            if (phrase.length() >= 2 && normalizedQuestion.contains(phrase)) {
                score += 20 + phrase.split("\\s+").length * 5;
            }
        }
        return score;
    }

    private List<String> splitKeywords(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("[,;\\n]+"))
            .map(String::trim)
            .filter(keyword -> !keyword.isBlank())
            .toList();
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
            .replace('đ', 'd')
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }

    public record FaqMatch(Long faqId, ChatbotResponse response) {
    }

    private record ScoredFaq(ChatbotFaq faq, int score) {
    }
}
