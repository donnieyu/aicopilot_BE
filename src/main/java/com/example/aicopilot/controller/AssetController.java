package com.example.aicopilot.controller;

import com.example.aicopilot.dto.asset.Asset;
import com.example.aicopilot.dto.asset.AssetDetailResponse;
import com.example.aicopilot.dto.asset.AssetSummaryResponse;
import com.example.aicopilot.service.AssetAnalysisService;
import com.example.aicopilot.service.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetAnalysisService assetAnalysisService;
    private final AssetRepository assetRepository;

    // [New] 1. 파일 업로드
    @PostMapping
    public ResponseEntity<?> uploadAsset(@RequestParam("file") MultipartFile file) {
        // 1. 등록 (ID 발급)
        Asset asset = assetAnalysisService.registerAsset(file);

        // 2. 비동기 분석 시작
        assetAnalysisService.processAssetAsync(asset.id(), file);

        // 업로드 직후에는 ID와 상태만 반환해도 충분함
        return ResponseEntity.ok(Map.of(
                "assetId", asset.id(),
                "status", asset.status()
        ));
    }

    // [New] 2. 전체 목록 조회 (Lightweight)
    @GetMapping
    public ResponseEntity<List<AssetSummaryResponse>> getAllAssets() {
        List<AssetSummaryResponse> summaries = assetRepository.findAll().stream()
                .map(AssetSummaryResponse::from)
                .sorted((a, b) -> Long.compare(b.uploadTime(), a.uploadTime())) // 최신순 정렬
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    // [Updated] 3. 단건 상세 조회 (Heavyweight)
    // 기존 getAssetStatus와 getAssetContent를 통합하여 상세 정보를 한 번에 제공하거나,
    // 폴링용 상태 확인 API를 유지할 수 있습니다. 여기서는 상세 조회용으로 명확히 합니다.
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssetDetail(@PathVariable String id) {
        return assetRepository.findById(id)
                .map(asset -> ResponseEntity.ok(AssetDetailResponse.from(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    // [Updated] 4. 상태 폴링용 API (Lightest)
    // 프론트엔드 폴링 부하를 줄이기 위해 최소한의 정보만 반환
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getAssetStatusOnly(@PathVariable String id) {
        return assetRepository.findById(id)
                .map(asset -> ResponseEntity.ok(Map.of(
                        "id", asset.id(),
                        "status", asset.status(),
                        "description", asset.description() != null ? asset.description() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // [Deprecated or Optional] 원문만 따로 필요한 경우 (상세 조회에 포함되어 있어 선택적)
    @GetMapping("/{id}/content")
    public ResponseEntity<?> getAssetContent(@PathVariable String id) {
        return assetRepository.findById(id)
                .map(asset -> ResponseEntity.ok(Map.of(
                        "content", asset.extractedText(),
                        "processDefinition", asset.processDefinitionJson()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}