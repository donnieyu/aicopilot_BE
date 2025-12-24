package com.example.aicopilot.dto.chat;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * AI 모델이 JSON 모드에서 인텐트를 반환하기 위한 래퍼 레코드입니다.
 */
public record IntentResponse(
        @JsonPropertyDescription("The classified intent of the user. Values: DESIGN, MODIFY, ANALYZE, GUIDE, CHAT")
        IntentType intent
) {}