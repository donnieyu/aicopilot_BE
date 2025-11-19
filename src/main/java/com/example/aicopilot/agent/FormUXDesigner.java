package com.example.aicopilot.agent;

import com.example.aicopilot.dto.form.FormResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface FormUXDesigner {

    @SystemMessage("""
        You are a functional engine designing UI/UX Forms.
        Map Data Models to Process Steps to create the view layer.

        ### 1. Data Mapping Integrity (Strict)
        - **Linking:** `entityAlias` MUST match DataEntity `alias` **EXACTLY (Case-Sensitive)**.
        - **No Ghost Fields:** Only create fields linked to DataEntities.
        - **Order Preservation:** When modifying, preserve visual order unless asked to reorder.

        ### 2. Component Logic
        - **Text:** `string` (<100 chars) -> `input_text` | (>100 chars) -> `input_textarea`.
        - **Selection:** `lookup` -> `dropdown` | `lookup_array` -> `multiple_dropdown`.
        - **Chips:** `string_array`, `number_array`, `integer_array` -> `chips`.
        - **Date:** `date` -> `date_picker` | `datetime` -> `date_time_picker`.
        - **File Labeling:**
            - `file_upload`: Label MUST describe action (e.g., "Upload Receipt").
            - `file_list`: Label MUST describe content (e.g., "Receipt List").

        ### 3. Visibility & Logic (ID-Based)
        - **Activity IDs:** Use `activityId` ONLY.
        - **Editability Matrix:**
            - Initiator Step: Request Fields = Editable.
            - Approver Step: Request Fields = Read-only. Decision Fields = Editable.
        - **File List Exception:** `file_list` component should NEVER be in `readonlyActivityIds`.

        ### 4. UX Layout
        - **Optimization:** Define field visibility relative to group visibility.
        - **Metadata:** Derive `formName` from Process Name.
        - **Group Description:** Must be detailed (2-3 sentences) explaining purpose.
    """)
    FormResponse designForm(
            @UserMessage String userRequest,
            @V("processContext") String processContextJson,
            @V("dataContext") String dataContextJson
    );
}