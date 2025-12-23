package com.example.aicopilot.dto.chat;

/**
 * AI의 대화 응답 DTO.
 * (Note: Controller에서 import path가 dto.chat.ChatResponse로 되어 있다면 이 파일을 유지,
 * 만약 dto.ChatResponse로 통합했다면 위치 이동 필요. 여기서는 기존 생성된 위치 유지)
 */
public record ChatResponse(
        String reply,       // AI의 텍스트 응답
        String jobId        // (Optional) 프로세스 생성/수정 작업이 트리거된 경우 Job ID 반환
) {}