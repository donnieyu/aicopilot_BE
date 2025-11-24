package com.example.aicopilot.agent;

import com.example.aicopilot.dto.dataEntities.DataEntitiesResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * 프로세스 정의를 기반으로 데이터 엔티티 모델을 설계하는 AI 에이전트 인터페이스.
 * <p>
 * 주요 변경사항 (v26.2.0):
 * <ul>
 * <li>데이터 원자화(Atomization) 전략 적용 (Task 단위가 아닌 Field 단위 추출)</li>
 * <li>데이터 리니지(Lineage) 추적을 위한 sourceNodeId 매핑 강화</li>
 * </ul>
 */
@AiService
public interface DataModeler {

    @SystemMessage("""
        You are a generic Data Architect extracting granular data requirements from a Business Process.
        
        ### GOAL
        Convert high-level process steps into **ATOMIC Data Entities**.
        Do NOT create a single entity for a whole task (e.g., avoid 'LeaveRequest' string). 
        Instead, explode it into specific fields (e.g., 'StartDate', 'EndDate', 'Reason').

        ### 1. Analyzing the Source (Critical)
        - Read the `userRequest` to understand the domain details.
        - Read the `processContextJson` to identify which Node needs which data.
        
        ### 2. Explosion Strategy (Atomization)
        For each `USER_TASK` (Form), imagine the actual UI form fields:
        - "Leave Request" -> needs `LeaveType` (Lookup), `StartDate` (Date), `EndDate` (Date), `Reason` (String).
        - "Expense Claim" -> needs `ExpenseDate`, `Category`, `Amount`, `ReceiptImage`.
        - "Approval" -> needs `Decision` (Lookup: Approve/Reject), `Comment` (String).

        ### 3. Smart Grouping
        - You MUST create `groups` in the response.
        - Group entities by their logical context or source node.
        - Example Group: "Leave Details" (containing StartDate, EndDate, Type).

        ### 4. Lineage (`sourceNodeId`)
        - Assign `sourceNodeId` strictly.
        - Input fields belong to the User Task that collects them.
        - Output fields (e.g., generated PDF url) belong to the Service Task.

        ### 5. Naming & Type Rules
        - `alias`: UpperCamelCase (e.g., `StartDate`).
        - `type`: Use precise types (`date`, `number`, `lookup`, `boolean`).
        - `lookupData`: If type is `lookup`, provide realistic items (e.g., LeaveType: Annual, Sick, Unpaid).
    """)
    // [FIX] @V 변수들을 조합할 UserMessage 템플릿 정의
    @UserMessage("""
        Here is the user request and the designed process structure.
        Analyze them to extract atomic data entities.

        [User Request]
        {{userRequest}}

        [Process Context (JSON)]
        {{processContextJson}}
        """)
    DataEntitiesResponse designDataModel(
            @V("userRequest") String userRequest,
            @V("processContextJson") String processContextJson
    );
}