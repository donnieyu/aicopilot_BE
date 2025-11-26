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

        ### 1. Swimlane Definition and Generation Rules (CRITICAL)
        
        #### 1. Definition and Role
        A **Swimlane** serves as a container that groups **Activities** within a process. It acts as a structural unit representing a specific organizational boundary or phase.
        * **Grouping:** It groups activities that belong to a specific department, function, or process phase.
        * **Process Stages:** The ordered collection of swimlanes defines the major high-level stages of the business process from start to finish.

        #### 2. Fields and Identification
        Each swimlane object MUST contain the following fields:
        * **`swimlaneId`**: A unique, machine-readable identifier for the swimlane (e.g., `'requester'`, `'legal_dept'`, `'phase_1'`).
        * **`name`**: A human-readable name for the swimlane to be displayed in the UI (e.g., `'Requester'`, `'Legal Department'`).

        #### 3. Critical Flow Generation Rules
        The flow of the process is strictly dictated by the order of swimlanes. You MUST adhere to the following logic to ensure a valid **Forward-Moving Sequence**:
        * **Sequence by Array Order**: The order of swimlanes in the `swimlanes` array dictates the sequence of process stages. The flow MUST progress from index `0` to index `N`.
        * **Next Pointer Logic (`nextSwimlaneId`)**:
            * The `nextSwimlaneId` of a swimlane MUST point to the `swimlaneId` of the **immediately subsequent** swimlane in the array.
            * **Termination:** The `nextSwimlaneId` for the **final** swimlane in the array MUST be `null`.
        * **Strict Forward Constraint**: A `nextSwimlaneId` **MUST NEVER** point to a swimlane that appears earlier in the `swimlanes` array. Cycles or backward steps at the swimlane level are strictly forbidden.

        #### 4. Atomicity and Integrity
        To ensure the logical integrity of the process model:
        * **Activity Requirement**: A swimlane CANNOT be empty. It **MUST** always contain at least one activity.
        * **Activity Flow**: Activities within a swimlane must link to other activities in the **same** swimlane or a **subsequent** swimlane. They cannot link to activities in a previous swimlane.

        ### 2. The "Explicit Gateway" Rule
        - **NO Implicit Branching:** Tasks (USER_TASK, SERVICE_TASK) can ONLY have one output (`nextActivityId`).
        - **Handling Approvals/Decisions:** - If a User Task is an approval, it MUST be immediately followed by an `EXCLUSIVE_GATEWAY`.
          - The Gateway checks the outcome variable (e.g., `approval_status`).
          - Structure: [Manager Approval Task] -> [Approval Check Gateway] -> (Branch to Next OR Branch back to Edit).

        ### 3. Node Configuration Strategy
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
          
        ### 4. ID & Flow Standards
        - `id`: **snake_case**.
        - `nextActivityId`: The default forward path. Use "node_end" for the final step.

        ### 5. Forbidden Explicit Nodes (CRITICAL)
        - **NO Manual Start/End Nodes:** The system automatically renders Start and End events.
        - **Do NOT** generate activities named "Start", "End", "Process Complete", "Stop", or "Finish".
        - **Termination:** To indicate the end of a process branch (e.g., after rejection), DO NOT create a node. Just set `targetActivityId` or `nextActivityId` to `"node_end"`.

        ### 6. Output Example (Correct Ordering)
        {
          "processName": "Leave Request",
          "swimlanes": [
             // Employee starts the process -> Must be first (Index 0)
             { "swimlaneId": "lane_emp", "name": "Employee", "nextSwimlaneId": "lane_mgr" },
             // Manager approves later -> Must be second (Index 1)
             { "swimlaneId": "lane_mgr", "name": "Manager", "nextSwimlaneId": null }
          ],
          "activities": [...]
        }
    """)
    ProcessResponse designProcess(String userRequest);
}