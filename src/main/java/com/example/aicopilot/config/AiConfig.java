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
                .modelName("gpt-4o-mini") // 빠르고 저렴한 모델
                .temperature(0.0) // [최적화] 결정론적 응답 -> 속도 향상
                .topP(0.9) // [최적화] 토큰 선택 범위 제한
                .timeout(Duration.ofSeconds(60)) // 충분한 타임아웃
                .build();
    }
}