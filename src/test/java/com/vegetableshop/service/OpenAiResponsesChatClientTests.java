package com.vegetableshop.service;

import com.vegetableshop.config.OpenAiChatProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OpenAiResponsesChatClientTests {

    @Test
    void requestUsesResponsesApiStructuredOutputAndDoesNotStoreConversation() {
        OpenAiResponsesChatClient client = new OpenAiResponsesChatClient(
            RestClient.create(), new ObjectMapper(), properties()
        );

        Map<String, Object> body = client.requestBody("Tư vấn món cho người ăn chay", List.of());

        assertEquals("gpt-5.4-nano", body.get("model"));
        assertEquals(false, body.get("store"));
        assertFalse(body.get("input").toString().isBlank());
        Map<?, ?> text = (Map<?, ?>) body.get("text");
        Map<?, ?> format = (Map<?, ?>) text.get("format");
        assertEquals("json_schema", format.get("type"));
        assertEquals(true, format.get("strict"));
    }

    @Test
    void extractsOnlyOutputTextFromResponsesPayload() {
        Map<String, Object> payload = Map.of(
            "output", List.of(Map.of(
                "type", "message",
                "content", List.of(Map.of(
                    "type", "output_text",
                    "text", "{\"answer\":\"Xin chào\"}"
                ))
            ))
        );

        assertEquals("{\"answer\":\"Xin chào\"}",
            OpenAiResponsesChatClient.extractOutputText(payload));
    }

    private OpenAiChatProperties properties() {
        return new OpenAiChatProperties(
            true, "test-key", "gpt-5.4-nano", "https://api.openai.com/v1", 12, 500
        );
    }
}
