package com.vegetableshop.dto;

import com.vegetableshop.entity.ChatbotInteraction;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public record ChatbotAdminDashboard(
    LocalDate fromDate,
    LocalDate toDate,
    long totalInteractions,
    long aiInteractions,
    long faqInteractions,
    long fallbackInteractions,
    long needsReview,
    long averageResponseTimeMs,
    long activeFaqs,
    List<DailyCount> dailyCounts,
    Page<ChatbotInteraction> interactions
) {
    public record DailyCount(LocalDate date, long count, int percentage) {
    }
}
