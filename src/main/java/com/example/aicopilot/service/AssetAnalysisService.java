package com.example.aicopilot.service;

import com.example.aicopilot.dto.analysis.AssetAnalysisResponse;
import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAnalysisService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    /**
     * Analyzes the uploaded file to extract a COMPLETE Process Definition.
     * This bypasses the Outliner stage and prepares data for direct Map Transformation.
     */
    public ProcessDefinition analyzeAssetToDefinition(MultipartFile file) {
        try {
            String mimeType = file.getContentType();
            String filename = file.getOriginalFilename();
            log.info("Analyzing asset for direct definition: {} ({})", filename, mimeType);

            UserMessage userMessage;

            if (mimeType != null && mimeType.startsWith("image/")) {
                // 1. Image Processing (Vision)
                String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
                userMessage = UserMessage.from(
                        TextContent.from(getBPMNAnalysisPrompt() + "\n\n[Instruction]\nAnalyze the attached BPMN Process Map image. Extract the exact flow structure into the requested JSON format."),
                        ImageContent.from(base64Image, mimeType)
                );
            } else {
                // 2. Text/Document Processing (Fallback for non-images)
                String extractedText = extractTextFromFile(file);
                userMessage = UserMessage.from(
                        getBPMNAnalysisPrompt() + "\n\n[Extracted File Content]\n" + extractedText
                );
            }

            // Call AI
            Response<AiMessage> response = chatLanguageModel.generate(userMessage);
            String jsonResponse = response.content().text();

            // Parse JSON (Clean up markdown code blocks if present)
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            // Map directly to ProcessDefinition (Topic + Steps)
            return objectMapper.readValue(jsonResponse, ProcessDefinition.class);

        } catch (Exception e) {
            log.error("Failed to analyze asset to definition", e);
            throw new RuntimeException("Asset analysis failed: " + e.getMessage());
        }
    }

    /**
     * Legacy method for simple text extraction (Outliner compatibility).
     */
    public AssetAnalysisResponse analyzeAsset(MultipartFile file) {
        return null;
    }

    // [Updated] Highly Specialized Prompt for BPMN/Flowchart Image Reverse Engineering
    // Includes detailed instructions for Confidence Calculation & Reasoning.
    private String getBPMNAnalysisPrompt() {
        return """
            You are an expert **BPMN 2.0 Reverse Engineer** and **Process Architect**.
            Your goal is to digitize a specific Process Map Image into a structured `ProcessDefinition` JSON with 100% fidelity.

            ### 🎯 CRITICAL MISSION: "NO SUMMARIZATION" & "NO REDUNDANT START"
            - **Do NOT** summarize the process (e.g., don't say "The system checks data").
            - **DO** extract **EVERY SINGLE NODE** visible in the image as a discrete step.
            - **EXCEPTION (Crucial):** Do NOT create separate steps for the visual 'Start Event' (Green Circle) or 'End Event' (Red Circle). The system automatically adds these. Only list the actual *Tasks* and *Gateways* between them.
            - **OCR Accuracy:** Read the **EXACT label text** inside each shape.

            ### 1. Visual Decoding Rules
            - **Role Inference (Icons):**
              - 👤 Person/User Icon -> Role: **"User"** (or specific name like 'Manager' if written).
              - ⚙️/🛢️/🖥️ System/SQL/Gear Icon -> Role: **"System"**.
              - ✉️ Envelope -> Role: **"System"** (Notification).
            - **Shape Semantics:**
              - ◇ **Diamond (Gateway):** Mandatory `type: "DECISION"`. Use the label text (e.g., "XOR1", "AND", "Check Letter").
              - □ **Rectangle (Task):** Mandatory `type: "ACTION"`.
              - 🕒 **Clock (Timer):** Treat as an `ACTION` step named "Wait for [Time]".

            ### 2. Sequence & Logic
            - Follow the arrows strictly.
            - **Gateways:** If you see a Gateway (Diamond), create a step for it.
            - **Loops:** If an arrow goes back, mention this in the `description`.

            ### 3. [NEW] Confidence & Evidence (Strict Calculation)
            - **Do NOT use a static value (e.g., 0.95) for all nodes.**
            - Calculate `confidence` (0.5 - 1.0) based on:
              1. **Text Clarity:** Is the text blurry? (-0.2)
              2. **Shape Ambiguity:** Is it clearly a diamond/rect? If hand-drawn or weird shape, lower score (-0.2).
              3. **Context:** Does the text match the shape? (e.g. 'Approval' in a Diamond is high confidence).
            - **`reason` Field (Required):** Briefly explain the score.
              - Examples: "Text is perfectly clear", "Blurry label, inferred from context", "Shape is ambiguous".

            ### 4. Output Data Structure (Strict JSON)
            Return ONLY the raw JSON. No markdown formatting.

            {
              "topic": "Exact Title from Image or Inferred Professional Name",
              "steps": [
                {
                  "stepId": "1", 
                  "name": "Pause Main Process SLAs",
                  "role": "System",
                  "description": "SQL Task to pause SLAs.",
                  "type": "ACTION",
                  "sourceRef": {
                    "fileId": "uploaded_asset",
                    "pageIndex": 0,
                    "rects": [{ "x": 10, "y": 20, "w": 15, "h": 10 }],
                    "confidence": 0.85,
                    "snippet": "Pause Main SLAs",
                    "reason": "Text clear but icon is small"
                  }
                }
                // ... EXTRACT EVERY NODE
              ]
            }
            """;
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
                    throw new IllegalArgumentException("Unsupported file type: " + extension);
            }
        }
    }

    private String parseExcel(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Read first sheet
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