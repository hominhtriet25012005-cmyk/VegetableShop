package com.vegetableshop.entity;

public enum ChatbotInteractionStatus {
    RESOLVED("Đã giải đáp"),
    NEEDS_REVIEW("Cần xem lại");

    private final String displayName;

    ChatbotInteractionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
