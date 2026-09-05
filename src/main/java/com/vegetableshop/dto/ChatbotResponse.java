package com.vegetableshop.dto;

import java.util.List;

public record ChatbotResponse(
    String message,
    List<ChatbotProductView> products,
    List<String> suggestions,
    String mode
) {

    public static final String AI_MODE = "AI";
    public static final String FAQ_MODE = "FAQ";
    public static final String RULE_BASED_MODE = "RULE_BASED";

    public ChatbotResponse(String message, List<ChatbotProductView> products, List<String> suggestions) {
        this(message, products, suggestions, RULE_BASED_MODE);
    }

    public static ChatbotResponse message(String message, List<String> suggestions) {
        return new ChatbotResponse(message, List.of(), List.copyOf(suggestions), RULE_BASED_MODE);
    }

    public static ChatbotResponse ai(
        String message,
        List<ChatbotProductView> products,
        List<String> suggestions
    ) {
        return new ChatbotResponse(message, List.copyOf(products), List.copyOf(suggestions), AI_MODE);
    }

    public static ChatbotResponse faq(String message, List<String> suggestions) {
        return new ChatbotResponse(message, List.of(), List.copyOf(suggestions), FAQ_MODE);
    }
}
