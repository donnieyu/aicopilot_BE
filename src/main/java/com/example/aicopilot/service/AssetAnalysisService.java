package com.example.aicopilot.service;

import com.example.aicopilot.dto.asset.Asset;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAnalysisService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final AssetRepository assetRepository;

    /**
     * [Phase 1] 비동기 파일 분석 프로세스 시작
     */
    @Async
    public void processAssetAsync(String assetId, MultipartFile file) {
        try {
            log.info("Starting async analysis for asset: {}", assetId);

            // 1. 텍스트 추출 (OCR or Parse)
            String extractedText = extractContent(file);

            // 2. AI Outliner 소스 생성 (설명 + JSON 분리 생성)
            AnalysisResult analysisResult = generateAnalysisResult(extractedText);

            // 3. 상태 업데이트
            // DTO 필드가 분리되었으므로 각각 명확하게 저장
            Asset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

            // ProcessDefinition 객체를 다시 JSON 문자열로 변환하여 저장
            String processJson = objectMapper.writeValueAsString(analysisResult.processDefinition());

            assetRepository.save(asset.withStatus(
                    Asset.AssetStatus.READY,
                    extractedText,
                    analysisResult.description(),
                    processJson
            ));

            log.info("Asset analysis completed: {}", assetId);

        } catch (Exception e) {
            log.error("Asset analysis failed", e);
            Asset asset = assetRepository.findById(assetId).orElse(null);
            if (asset != null) {
                // 실패 시 에러 메시지를 description에 저장
                assetRepository.save(asset.withFailure("Analysis failed: " + e.getMessage()));
            }
        }
    }

    // 파일 초기 등록
    public Asset registerAsset(MultipartFile file) {
        String id = UUID.randomUUID().toString();
        // 정적 팩토리 메서드로 초기화
        Asset asset = Asset.create(id, file.getOriginalFilename(), file.getContentType(), file.getSize());
        // 상태를 ANALYZING으로 변경 (비동기 시작 전)
        asset = asset.withStatus(Asset.AssetStatus.ANALYZING, null, null, null);

        assetRepository.save(asset);
        return asset;
    }

    private String extractContent(MultipartFile file) throws IOException {
        String mimeType = file.getContentType();

        if (mimeType != null && mimeType.startsWith("image/")) {
            return "Image content analysis not fully implemented yet. (Mock Text)";
        }
        return extractTextFromFile(file);
    }

    // 분석 결과를 담을 내부 DTO
    record AnalysisResult(String description, ProcessDefinition processDefinition) {}

    /**
     * [Updated] 문서 내용을 분석하여 사용자 친화적인 설명(Text)과 구조화된 데이터(JSON)를 동시에 생성합니다.
     */
    private AnalysisResult generateAnalysisResult(String text) {
        if (text == null || text.isEmpty()) return new AnalysisResult("No content extracted.", null);

        // 너무 긴 텍스트는 잘라서 처리
        String content = text.length() > 15000 ? text.substring(0, 15000) + "..." : text;

        UserMessage msg = UserMessage.from("""
            You are a 'Business Process Analyst'.
            Analyze the provided document and extract two things:
            1. A natural language summary explaining the process flow (for the user).
            2. A structured Process Definition JSON (for the system).
            
            [Input Content]
            """ + content + """
            
            ### Instructions
            1. **Description**: Write a clear, step-by-step explanation of the process in **English**. Start with "This document outlines..."
            2. **Structure**: Convert the flow into a JSON object compatible with `ProcessDefinition`.
            
            ### Output Format (Strict JSON)
            Return a JSON object with two keys:
            {
              "description": "This document outlines the vacation request procedure. When an employee...", 
              "processDefinition": {
                "topic": "Inferred Process Title",
                "steps": [
                  {
                    "stepId": "1",
                    "name": "Step Title",
                    "role": "Actor",
                    "description": "Detail",
                    "type": "ACTION"
                  }
                ]
              }
            }
            
            IMPORTANT: Return ONLY raw JSON. No markdown.
            """);

        try {
            Response<AiMessage> response = chatLanguageModel.generate(msg);
            String jsonResponse = response.content().text();

            // Markdown Cleanup
            if (jsonResponse.contains("```json")) {
                jsonResponse = jsonResponse.replace("```json", "").replace("```", "");
            } else if (jsonResponse.contains("```")) {
                jsonResponse = jsonResponse.replace("```", "");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            String description = root.path("description").asText();
            JsonNode defNode = root.path("processDefinition");
            ProcessDefinition definition = objectMapper.treeToValue(defNode, ProcessDefinition.class);

            return new AnalysisResult(description, definition);

        } catch (Exception e) {
            log.warn("Failed to generate analysis result", e);
            return new AnalysisResult("Analysis failed: " + e.getMessage(), null);
        }
    }

    // --- 기존 로직 유지 ---
    public ProcessDefinition analyzeAssetToDefinition(MultipartFile file) {
        try {
            String extractedText = extractTextFromFile(file);
            return null; // Legacy placeholder
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }

        try (InputStream is = file.getInputStream()) {
            switch (extension) {
                case "xlsx":
                case "xls":
                    return parseExcel(is);
                case "pdf":
                    return parsePdf(is);
                case "csv":
                case "txt":
                case "md":
                    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                default:
                    return "Unsupported file type.";
            }
        }
    }

    private String parseExcel(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(cell.toString()).append("\t");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String parsePdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}