package com.vegetableshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_interactions")
public class ChatbotInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_source", nullable = false, length = 30)
    private ChatbotResponseSource responseSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatbotInteractionStatus status;

    @Column(name = "product_count", nullable = false)
    private Integer productCount = 0;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_faq_id")
    private ChatbotFaq matchedFaq;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public ChatbotResponseSource getResponseSource() { return responseSource; }
    public void setResponseSource(ChatbotResponseSource responseSource) { this.responseSource = responseSource; }
    public ChatbotInteractionStatus getStatus() { return status; }
    public void setStatus(ChatbotInteractionStatus status) { this.status = status; }
    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public ChatbotFaq getMatchedFaq() { return matchedFaq; }
    public void setMatchedFaq(ChatbotFaq matchedFaq) { this.matchedFaq = matchedFaq; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
