package com.example.aicopilot.service;

import com.example.aicopilot.dto.analysis.AssetAnalysisResponse;
import com.example.aicopilot.dto.definition.ProcessDefinition; // [New] Import
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

    // [Updated] Highly Specialized Prompt for BPMN/Flowchart Image Reverse Engineering with Strict Swimlane Detection
    private String getBPMNAnalysisPrompt() {
        return """
            You are an expert **BPMN 2.0 Reverse Engineer** and **Process Architect**.
            Your goal is to digitize a specific Process Map Image into a structured `ProcessDefinition` JSON with 100% fidelity.

            ### 🎯 MISSION: "Vision to Structured Data with Swimlanes"
            Extract the business logic from the image into a linear list of steps.
            **CRITICAL:** You MUST identify **Swimlanes (Lanes)** in the image to determine the correct `role` for each step. This is the primary method for role assignment.
            Since the output is a linear list, you MUST describe the flow connections (branching, looping) inside the `description` field so the downstream system can reconstruct the graph.

            ### 1. Visual Decoding Rules (Vision Analysis)
            - **Hierarchy Detection (Pool vs Lane):**
              - Ignore the outermost container title (e.g., "Pool 1", "Main Process") if it contains inner subdivisions.
              - Focus on the **Inner Containers (Lanes)** that divide the chart horizontally or vertically.
              - **Read Headers:** Look for specific headers like **"Employee", "Manager", "HR", "Finance"**. These are the valid `role` values.
            
            - **Spatial Mapping:**
              - For every Task or Gateway node, visually determine **which Lane's boundary it falls inside**.
              - Assign that Lane's header text as the `role` for the step.
              - Example: If a "Approve" node is visually inside the "Manager" column, its role MUST be "Manager".

            - **Shape Semantics:**
              - ◇ **Diamond (Gateway):** Mandatory `type: "DECISION"`. Use the label text (e.g., "XOR1", "AND", "Check Letter").
              - □ **Rectangle (Task):** Mandatory `type: "ACTION"`.
              - 🕒 **Clock (Timer):** Treat as an `ACTION` step named "Wait for [Time]".

            ### 2. Sequence & Logic
            - Follow the arrows strictly.
            - **Gateways:** If you see a Gateway (Diamond), create a step for it. In the `description`, explicitly state the conditions found on the outgoing arrows.
              - *Example:* "Exclusive Gateway. If 'Approved' -> Go to Payment. If 'Rejected' -> Return to Request."
            - **Rejection/Loops:** If an arrow goes BACK to a previous step (especially across Swimlanes), mention this in the `description`.
              - *Example:* "Manager review task. If rejected, the process loops back to the 'Leave Request Application' step in the Employee lane."

            ### 3. [UPDATED] Confidence Scoring Algorithm (Deduction Method)
            **Do NOT default to 0.9 or 0.95.** Start with a score of **1.0** and apply deductions based on visual evidence.
            
            **[Rules for Deduction]**
            1. **Text Legibility (-0.1 to -0.3):**
               - Slightly blurry or small font? **-0.1**
               - Hard to read, guessed some letters? **-0.2**
               - Illegible, purely inferred from context? **-0.3**
            2. **Shape Ambiguity (-0.1 to -0.2):**
               - Shape boundary is unclear or hand-drawn style? **-0.15**
               - Ambiguous whether it's a Gateway or Task? **-0.2**
            3. **Context Mismatch (-0.2):**
               - Text says "Decision" but shape is a Rectangle? **-0.2**
               - Text says "Submit" but shape is a Diamond? **-0.2**

            **[Reasoning Requirement]**
            - The `reason` field MUST explain the calculation.
            - Example: "Text is slightly blurry (-0.1), but shape is clear." -> Final Score: 0.9
            - Example: "Text illegible (-0.3) and shape ambiguous (-0.1)." -> Final Score: 0.6

            ### 4. Output Data Structure (Strict JSON)
            Return ONLY the raw JSON. No markdown formatting.

            {
              "topic": "Exact Title from Image or Inferred Professional Name",
              "steps": [
                {
                  "stepId": "1", 
                  "name": "Leave Request Application",
                  "role": "Employee", // Extracted from "Employee" Lane
                  "description": "Employee submits the leave request.",
                  "type": "ACTION",
                  "sourceRef": {
                    "fileId": "uploaded_asset",
                    "pageIndex": 0,
                    "rects": [{ "x": 10, "y": 20, "w": 15, "h": 10 }],
                    "confidence": 0.95,
                    "snippet": "Leave Request Application",
                    "reason": "Text is perfectly clear and sharp."
                  }
                },
                {
                  "stepId": "2",
                  "name": "Manager Review",
                  "role": "Manager", // Extracted from "Manager" Lane
                  "description": "Manager reviews the request. If 'Approved' -> Go to HR Review. If 'Rejected' -> Loop back to Leave Request Application.",
                  "type": "DECISION",
                  "sourceRef": {
                    "fileId": "uploaded_asset",
                    "pageIndex": 0,
                    "rects": [{ "x": 30, "y": 20, "w": 15, "h": 10 }],
                    "confidence": 0.75,
                    "snippet": "Mgr Rvw", 
                    "reason": "Text is abbreviated and slightly blurry (-0.15), context inferred (-0.1)."
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