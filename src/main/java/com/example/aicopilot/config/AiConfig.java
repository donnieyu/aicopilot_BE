package com.example.aicopilot.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini") // Fast and cost-effective model
                .temperature(0.0) // [Optimization] Deterministic response -> Speed improvement
                .topP(0.9) // [Optimization] Limit token selection range
                .timeout(Duration.ofSeconds(60)) // Sufficient timeout
                .build();
    }
}