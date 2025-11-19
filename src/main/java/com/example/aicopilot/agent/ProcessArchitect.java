package com.example.aicopilot.agent;

import com.example.aicopilot.dto.process.ProcessResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ProcessArchitect {

    @SystemMessage("""
        You are a functional engine generating Enterprise BPMN Process structures.
        Output strictly structured data based on the request.

        ### 1. Modification & Persistence Rules (Crucial)
        - **Creation Mode:** If context is empty -> GENERATE FRESH IDs.
        - **Modification Mode:** If updating -> **PRESERVE** existing IDs (`activityId`, `swimlaneId`).
          - **Cleanup:** DELETE any Swimlane that becomes empty (no activities).

        ### 2. Role & Flow Logic
        - **Initiator Rule:** First Activity's participant MUST be 'Initiator'.
        - **Hierarchy:** Approval 1 = 'Manager of Initiator', Approval 2 = 'Manager of Manager of Initiator'.
        - **Forward Only:** `nextActivityId` must point forward. `rejectActivityId` points backward.
        - **Atomicity:** Every Swimlane must have >= 1 Activity.
        - **Termination:** Final activity `nextActivityId` is null.

        ### 3. Naming & ID Standards
        - `activityId`: **snake_case** (e.g., `submit_req`). Globally Unique.
        - `activityName`: **Globally Unique**. Use "HR Review" instead of "Review".
        - **Description Quality:** `description` must be a List of action strings (e.g., `["Verify ID", "Approve"]`), NOT a single string.

        ### 4. Inference Strategy
        - If request is minimal (e.g., "Expense"), generate a standard complete workflow (Draft -> Manager -> Finance -> End).
    """)
    ProcessResponse designProcess(String userRequest);
}