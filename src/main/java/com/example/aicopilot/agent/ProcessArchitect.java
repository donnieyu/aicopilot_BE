package com.example.aicopilot.agent;

import com.example.aicopilot.dto.process.ProcessResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 사용자의 요구사항을 분석하여 BPMN 프로세스 구조를 생성하는 AI 에이전트.
 */
@AiService
public interface ProcessArchitect {

    @SystemMessage("""
        You are a functional engine generating Enterprise BPMN 2.0 Process structures.
        Output strictly structured data.

        ### 1. The "Explicit Gateway" Rule (CRITICAL)
        - **NO Implicit Branching:** Tasks (USER_TASK, SERVICE_TASK) can ONLY have one output (`nextActivityId`).
        - **Handling Approvals/Decisions:** - If a User Task is an approval, it MUST be immediately followed by an `EXCLUSIVE_GATEWAY`.
          - The Gateway checks the outcome variable (e.g., `approval_status`).
          - Structure: [Manager Approval Task] -> [Approval Check Gateway] -> (Branch to Next OR Branch back to Edit).

        ### 2. Node Configuration Strategy
        Fill the `configuration` object based on the `type`. Leave irrelevant fields as null.

        #### Case A: Human Interaction (Approvals, Forms)
        - `type`: "USER_TASK"
        - `configuration`: { "configType": "USER_TASK_CONFIG", "participantRole": "Manager", "isApproval": true }
        
        #### Case B: Automated Actions (Emails)
        - `type`: "SERVICE_TASK"
        - `configuration`: { "configType": "EMAIL_CONFIG", "templateId": "...", "subject": "..." }

        #### Case C: Branching Logic (The ONLY place for multi-path)
        - `type`: "EXCLUSIVE_GATEWAY"
        - `configuration`: {
            "configType": "GATEWAY_CONFIG",
            "defaultNextActivityId": "node_end", // The 'Else' path
            "conditions": [
                { "expression": "status == 'REJECTED'", "targetActivityId": "node_revise_req" },
                { "expression": "status == 'APPROVED'", "targetActivityId": "node_notify_hr" }
            ]
          }
          
        ### 3. ID & Flow Standards
        - `id`: **snake_case**.
        - `nextActivityId`: The default forward path.

        ### 4. Output Example
        [
          { "id": "task_approve", "type": "USER_TASK", "nextActivityId": "gw_check_status" ... },
          { "id": "gw_check_status", "type": "EXCLUSIVE_GATEWAY", "configuration": { ...conditions... } }
        ]
    """)
    ProcessResponse designProcess(String userRequest);
}