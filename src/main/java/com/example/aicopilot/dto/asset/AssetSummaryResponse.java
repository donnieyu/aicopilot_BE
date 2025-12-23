package com.example.aicopilot.dto.asset;

import com.example.aicopilot.dto.asset.Asset.AssetStatus;

/**
 * 목록 조회용 경량 DTO
 * 전체 텍스트나 프로세스 정의 JSON은 포함하지 않습니다.
 */
public record AssetSummaryResponse(
        String id,
        String fileName,
        String contentType,
        long size,
        AssetStatus status,
        String description, // 사용자용 요약 설명은 목록에서도 유용하므로 포함
        long uploadTime
) {
    public static AssetSummaryResponse from(Asset asset) {
        return new AssetSummaryResponse(
                asset.id(),
                asset.fileName(),
                asset.contentType(),
                asset.size(),
                asset.status(),
                asset.description(),
                asset.uploadTime()
        );
    }
}