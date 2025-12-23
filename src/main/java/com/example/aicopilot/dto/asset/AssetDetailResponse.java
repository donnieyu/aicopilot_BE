package com.example.aicopilot.dto.asset;

import com.example.aicopilot.dto.asset.Asset.AssetStatus;

/**
 * 상세 조회용 DTO
 * 원문 텍스트와 프로세스 정의 JSON을 포함합니다.
 */
public record AssetDetailResponse(
        String id,
        String fileName,
        String contentType,
        long size,
        AssetStatus status,
        String description,
        String extractedText,       // 상세 정보
        String processDefinitionJson, // 상세 정보
        long uploadTime
) {
    public static AssetDetailResponse from(Asset asset) {
        return new AssetDetailResponse(
                asset.id(),
                asset.fileName(),
                asset.contentType(),
                asset.size(),
                asset.status(),
                asset.description(),
                asset.extractedText(),
                asset.processDefinitionJson(),
                asset.uploadTime()
        );
    }
}