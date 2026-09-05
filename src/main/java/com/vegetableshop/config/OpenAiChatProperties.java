package com.vegetableshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chatbot.ai")
public record OpenAiChatProperties(
    boolean enabled,
    String apiKey,
    String model,
    String baseUrl,
    int timeoutSeconds,
    int maxOutputTokens
) {
}
