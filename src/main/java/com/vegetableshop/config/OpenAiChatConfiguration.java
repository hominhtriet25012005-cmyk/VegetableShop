package com.vegetableshop.config;

import com.vegetableshop.service.AiChatClient;
import com.vegetableshop.service.OpenAiResponsesChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiChatProperties.class)
public class OpenAiChatConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.chatbot.ai.enabled", havingValue = "true")
    AiChatClient openAiChatClient(OpenAiChatProperties properties, ObjectMapper objectMapper) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                "CHATBOT_AI_ENABLED=true requires a non-empty OPENAI_API_KEY"
            );
        }

        int timeoutSeconds = Math.max(3, properties.timeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient restClient = RestClient.builder()
            .baseUrl(withoutTrailingSlash(properties.baseUrl()))
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey().trim())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(requestFactory)
            .build();
        return new OpenAiResponsesChatClient(restClient, objectMapper, properties);
    }

    private String withoutTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
