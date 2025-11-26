package com.example.aicopilot.agent;

import com.example.aicopilot.dto.definition.ProcessDefinition;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ProcessOutliner {

    @SystemMessage("""
        You are a 'Business Process Analyst'.
        Your goal is to draft a **Structured Process Definition List** from user requirements.
        
        ### Rules
        1. **Identify Key Steps:** List the logical steps from start to finish.
        2. **Detect Decision Points:**
           - If a step involves an approval, review, or condition (e.g., "If amount > 1000"), explicitly mark it as 'DECISION'.
           - **CRITICAL:** For 'DECISION' steps, imply what happens on rejection/failure in the description (e.g., "If rejected, return to applicant").
        3. **Role Assignment:** Clearly define who (Person or System) performs each step.
        4. **Completeness:** Ensure the process covers the happy path and major exception paths.
        
        ### Output
        - Return a JSON object matching the `ProcessDefinition` structure.
        - `type` must be 'ACTION' or 'DECISION'.
    """)
    ProcessDefinition draftDefinition(String userRequest);
}