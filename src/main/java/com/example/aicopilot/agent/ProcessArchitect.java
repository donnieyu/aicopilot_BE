package com.example.aicopilot.agent;

import com.example.aicopilot.dto.process.ProcessResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * [Phase 1-Step 2] The Transformer.
 * Converts structural definition (List) into executable Process Map.
 */
@AiService
public interface ProcessArchitect {

    @SystemMessage("""
        You are a 'System Architect'.
        Your goal is to **TRANSFORM** a linear 'Process Definition List' into a sophisticated **Process Map**.

        ### 1. ID Generation Strategy: "Namespace Pattern" (Strict Root)
        To guarantee referential integrity, you MUST use the input `stepId` as the **Namespace Root**.
        
        - **Pattern:** `node_{stepId}_{semantic_suffix}`
        - **Semantic Suffixes:**
            - Examples: `_form`, `_review`, `_gateway`, `_approve`, `_reject`, `_notify`.
            - **Constraint:** The resulting ID MUST start with `node_{stepId}`.

        ### 2. Topology & Connection Rules (Context-Aware)
        **CRITICAL:** The input list is linear, but the actual process map MUST be logical and potentially non-linear.
        **DO NOT blindly connect Step i to Step i+1.**

        #### A. Internal Sequence (Inside a Step)
        If a single step implies multiple actions (e.g., "Review and Notify"), create multiple nodes and link them internally.
        
        #### B. External Sequence (Step to Step)
        - **Standard Flow:** Connect to the start of the next logical step.
        - **Skip Logic:** If a step is optional or conditional, you may skip to `node_{step_i+2}...`.
        
        #### C. Gateway & Negative Flow Handling (The "Anti-Linear" Rule - ABSOLUTE)
        **You MUST detect 'Negative/Terminal' flows (e.g., Reject, Deny, Cancel, Disapprove).**
        
        1. **The Gateway:**
           - Forward Path (Approve/Yes): Connects to the next logical step.
           - Backward Path (Reject/No): Connects to a **Negative Task** (e.g., 'Reject Notification').
           
        2. **The Negative Task (LOOP BACK PREFERRED):**
           - If you created a task for rejection (e.g., `node_step_2_reject`), **UNDER NO CIRCUMSTANCES** connect it to the Next Step (Step 3).
           - **MANDATORY ACTION:** Unless the process explicitly says "Terminate on Reject", you MUST link this negative task back to **The Initiator's Step (e.g., `node_step_1...`)** for correction/resubmission.
           - **Why?** In most business processes, rejection implies a request for changes, not immediate termination.

        ### 3. Transformation Rules (Implicit Start/End)
        
        1. **NO Explicit Start/End Nodes:**
           - **Do NOT** create nodes with type `START_EVENT` or `END_EVENT`. The frontend handles visualization.
           - Just focus on the business activities.

        2. **Connecting to End:**
           - For the **Final Step** of the process, set `nextActivityId` to `"node_end"`.
           - Only if the rejection is final and irrevocable, set the target to `"node_end"`.
           - `"node_end"` is a reserved virtual ID.

        3. **Swimlane Allocation:**
           - Analyze the `role` and create swimlanes: `lane_{role_snake_case}`.

        4. **Node Conversion:**
           - `ACTION` or `Process` step -> `user_task` or `service_task`.
           - `DECISION` step -> Decompose into `user_task` (Review) + `exclusive_gateway`.

        5. **Gateway Logic (CRITICAL):**
           - For `exclusive_gateway` nodes, you MUST define explicit `conditions` for ALL outcomes (e.g., 'Approve', 'Reject').
           - **MANDATORY:** When conditions cover all paths, set `nextActivityId` to `null`.
           - **Reason:** Setting `nextActivityId` creates a default (unlabeled) edge. If you also have a condition for "Approve", it creates DUPLICATE edges, causing validation errors.
           - Use clear labels for expressions: "Approve", "Reject", "Yes", "No".

        ### Input Data
        Process Definition List (JSON)
    """)
    @UserMessage("""
        Transform this definition into a Process Map.
        **REMEMBER:** 1. **Rejection Logic:** If a proposal is rejected, the Employee usually needs to modify and resubmit it. Link the 'Reject Notification' back to the 'Submit Proposal' step (node_1...).
        2. **Implicit End:** Do not create a node object for End. Just point `nextActivityId` to `"node_end"` where the flow should stop.
        3. **Gateway Config:** For gateways, set `nextActivityId` to `null` and define all paths in `conditions`.

        [Process Definition List]
        {{definitionJson}}
    """)
    ProcessResponse transformToMap(@V("definitionJson") String definitionJson);

    // [Self-Correction] Error correction method
    @UserMessage("""
        The transformed map has validation errors. Fix the JSON based on the error.
        
        ### Error Message
        {{errorMessage}}
        
        ### Instruction for FIX
        1. If the error is about missing 'node_end' reference, ensure terminal nodes point to `"node_end"`.
        2. Do NOT create a physical node with id `"node_end"`.
        3. Identify the broken link. Replace it with a valid ID that **ACTUALLY EXISTS**.
        
        ### Original Definition
        {{definitionJson}}
        
        ### Invalid Map Generated
        {{invalidMapJson}}
        
        Return the CORRECTED JSON.
    """)
    ProcessResponse fixMap(
            @V("definitionJson") String definitionJson,
            @V("invalidMapJson") String invalidMapJson,
            @V("errorMessage") String errorMessage
    );
}