package com.vegetableshop.dto;

import java.util.List;

public record AiChatDecision(
    String intent,
    String answer,
    List<Long> productIds,
    List<String> suggestions
) {
}
