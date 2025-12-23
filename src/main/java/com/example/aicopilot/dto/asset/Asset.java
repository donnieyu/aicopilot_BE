package com.example.aicopilot.dto.asset;

public record Asset(
        String id,
        String fileName,
        String contentType,
        long size,
        AssetStatus status,
        String extractedText,       // 1. OCR 원문 (Context 주입용)
        String description,         // 2. 사용자용 자연어 요약 (UI 표시용, 영어 또는 한국어)
        String processDefinitionJson, // 3. 구조화된 프로세스 데이터 (시스템/생성용 JSON 문자열)
        long uploadTime
) {
    public enum AssetStatus {
        UPLOADING,
        ANALYZING,
        READY,
        FAILED
    }

    // 상태 변경을 위한 유틸리티 메서드
    // description과 processDefinitionJson을 명확하게 분리하여 업데이트
    public Asset withStatus(AssetStatus newStatus, String text, String description, String processDefinitionJson) {
        return new Asset(id, fileName, contentType, size, newStatus, text, description, processDefinitionJson, uploadTime);
    }

    // 초기 생성 시 편의 메서드 (내용 없음)
    public static Asset create(String id, String fileName, String contentType, long size) {
        return new Asset(id, fileName, contentType, size, AssetStatus.UPLOADING, null, null, null, System.currentTimeMillis());
    }

    // 실패 시 편의 메서드
    public Asset withFailure(String errorMessage) {
        // 에러 메시지를 description에 저장하여 사용자에게 알림
        return new Asset(id, fileName, contentType, size, AssetStatus.FAILED, null, errorMessage, null, uploadTime);
    }
}