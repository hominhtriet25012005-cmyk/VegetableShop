package com.vegetableshop.entity;

public enum ChatbotResponseSource {
    MANAGED_FAQ("FAQ quản trị"),
    AI("OpenAI"),
    RULE_BASED("Nội bộ 21A"),
    AI_FALLBACK("Dự phòng khi AI lỗi");

    private final String displayName;

    ChatbotResponseSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
