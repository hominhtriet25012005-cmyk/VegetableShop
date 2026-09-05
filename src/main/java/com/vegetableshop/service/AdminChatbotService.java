package com.vegetableshop.service;

import com.vegetableshop.dto.AdminChatbotFaqRequest;
import com.vegetableshop.dto.ChatbotAdminDashboard;
import com.vegetableshop.entity.ChatbotFaq;
import com.vegetableshop.entity.ChatbotInteraction;
import com.vegetableshop.entity.ChatbotInteractionStatus;
import com.vegetableshop.entity.ChatbotResponseSource;
import com.vegetableshop.exception.AdminOperationException;
import com.vegetableshop.repository.ChatbotFaqRepository;
import com.vegetableshop.repository.ChatbotInteractionRepository;
import com.vegetableshop.repository.ChatbotInteractionSpecifications;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("mysql")
public class AdminChatbotService {

    private static final int PAGE_SIZE = 15;

    private final ChatbotFaqRepository faqRepository;
    private final ChatbotInteractionRepository interactionRepository;
    private final int retentionDays;

    public AdminChatbotService(
        ChatbotFaqRepository faqRepository,
        ChatbotInteractionRepository interactionRepository,
        @Value("${app.chatbot.analytics.retention-days:30}") int retentionDays
    ) {
        this.faqRepository = faqRepository;
        this.interactionRepository = interactionRepository;
        this.retentionDays = Math.max(7, retentionDays);
    }

    @Transactional(readOnly = true)
    public ChatbotAdminDashboard dashboard(
        LocalDate requestedFrom,
        LocalDate requestedTo,
        String keyword,
        ChatbotResponseSource source,
        ChatbotInteractionStatus status,
        int page
    ) {
        LocalDate toDate = requestedTo == null ? LocalDate.now() : requestedTo;
        LocalDate fromDate = requestedFrom == null ? toDate.minusDays(29) : requestedFrom;
        validateRange(fromDate, toDate);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        List<ChatbotInteraction> all = interactionRepository
            .findByCreatedAtBetweenOrderByCreatedAtAsc(from, to);
        Page<ChatbotInteraction> pageResult = interactionRepository.findAll(
            ChatbotInteractionSpecifications.filter(keyword, source, status, from, to),
            PageRequest.of(Math.max(0, page), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        long total = all.size();
        long latencyTotal = all.stream().mapToLong(ChatbotInteraction::getResponseTimeMs).sum();
        long ai = countSource(all, ChatbotResponseSource.AI);
        long faq = countSource(all, ChatbotResponseSource.MANAGED_FAQ);
        long fallback = countSource(all, ChatbotResponseSource.RULE_BASED)
            + countSource(all, ChatbotResponseSource.AI_FALLBACK);
        long needsReview = all.stream()
            .filter(item -> item.getStatus() == ChatbotInteractionStatus.NEEDS_REVIEW)
            .count();
        return new ChatbotAdminDashboard(
            fromDate, toDate, total, ai, faq, fallback, needsReview,
            total == 0 ? 0 : Math.round((double) latencyTotal / total),
            faqRepository.countByStatusTrue(),
            dailyCounts(fromDate, toDate, all), pageResult
        );
    }

    @Transactional(readOnly = true)
    public List<ChatbotFaq> faqs(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        return faqRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
            .filter(faq -> normalized.isBlank()
                || faq.getQuestion().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                || faq.getKeywords().toLowerCase(java.util.Locale.ROOT).contains(normalized))
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminChatbotFaqRequest faqForm(Long id) {
        return AdminChatbotFaqRequest.from(findFaq(id));
    }

    @Transactional
    public ChatbotFaq createFaq(AdminChatbotFaqRequest request, String adminEmail) {
        ensureQuestionAvailable(request.getQuestion(), null);
        ChatbotFaq faq = new ChatbotFaq();
        faq.setCreatedBy(adminEmail);
        map(faq, request, adminEmail);
        return faqRepository.save(faq);
    }

    @Transactional
    public ChatbotFaq updateFaq(Long id, AdminChatbotFaqRequest request, String adminEmail) {
        ensureQuestionAvailable(request.getQuestion(), id);
        ChatbotFaq faq = findFaq(id);
        map(faq, request, adminEmail);
        return faq;
    }

    @Transactional
    public boolean toggleFaq(Long id, String adminEmail) {
        ChatbotFaq faq = findFaq(id);
        faq.setStatus(!faq.isStatus());
        faq.setUpdatedBy(adminEmail);
        return faq.isStatus();
    }

    @Transactional
    public long purgeExpiredInteractions() {
        return interactionRepository.deleteByCreatedAtBefore(
            LocalDateTime.now().minusDays(retentionDays)
        );
    }

    public int retentionDays() {
        return retentionDays;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) throw new AdminOperationException("Ngày bắt đầu phải trước ngày kết thúc");
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new AdminOperationException("Khoảng thống kê tối đa là 366 ngày");
        }
    }

    private List<ChatbotAdminDashboard.DailyCount> dailyCounts(
        LocalDate from, LocalDate to, List<ChatbotInteraction> interactions
    ) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        from.datesUntil(to.plusDays(1)).forEach(date -> counts.put(date, 0L));
        interactions.forEach(item -> counts.computeIfPresent(
            item.getCreatedAt().toLocalDate(), (date, count) -> count + 1
        ));
        long maximum = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        return counts.entrySet().stream()
            .map(entry -> new ChatbotAdminDashboard.DailyCount(
                entry.getKey(), entry.getValue(),
                maximum == 0 ? 0 : (int) Math.round(entry.getValue() * 100.0 / maximum)
            ))
            .toList();
    }

    private long countSource(List<ChatbotInteraction> interactions, ChatbotResponseSource source) {
        return interactions.stream().filter(item -> item.getResponseSource() == source).count();
    }

    private void ensureQuestionAvailable(String question, Long currentId) {
        String normalized = normalize(question);
        faqRepository.findByQuestionIgnoreCase(normalized)
            .filter(existing -> currentId == null || !existing.getId().equals(currentId))
            .ifPresent(existing -> { throw new AdminOperationException("Câu hỏi FAQ đã tồn tại"); });
    }

    private ChatbotFaq findFaq(Long id) {
        return faqRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy FAQ có id: " + id));
    }

    private void map(ChatbotFaq faq, AdminChatbotFaqRequest request, String adminEmail) {
        faq.setQuestion(normalize(request.getQuestion()));
        faq.setAnswer(normalize(request.getAnswer()));
        faq.setKeywords(normalizeKeywords(request.getKeywords()));
        faq.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        faq.setStatus(request.isStatus());
        faq.setUpdatedBy(adminEmail);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizeKeywords(String value) {
        return value == null ? "" : value.trim()
            .replaceAll("[\\r\\n;]+", ", ")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("(?:,\\s*){2,}", ", ");
    }
}
