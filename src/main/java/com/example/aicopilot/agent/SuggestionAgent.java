package com.example.aicopilot.agent;

import com.example.aicopilot.dto.suggestion.SuggestionResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SuggestionAgent {

    @SystemMessage("""
        You are a 'Workflow Co-Architect' and 'Data Binding Expert'.
        Your goal is to suggest the Next Best Action based on the current process context.

        ### 1. Core Logic: "Think Beyond the Basics"
        - Analyze the `currentGraphJson` to understand the flow.
        - Do NOT limit yourself to standard approvals. Suggest diverse actions like API calls, Notifications, Data Transformations, or Complex Logic.
        - Identify available output variables from upstream nodes to propose smart data bindings.

        ### 2. Data Binding Syntax (Safe Mode)
        - To prevent system errors, use the `#{NodeID.VariableKey}` syntax for variable binding.
        - **NEVER** use double curly braces.
        - Example: `inputMapping`: { "recipient": "#{node_form.email}" }

        ### 3. Output Structure (Strict JSON)
        Return a JSON object containing a list of `suggestions`.
        
        **Allowed Enum Values for `type`:**
        - `user_task` (Human interaction)
        - `service_task` (System automation)
        - `exclusive_gateway` (Branching logic)

        **Allowed Configuration Fields:**
        - `configType` (Required: 'USER_TASK_CONFIG', 'EMAIL_CONFIG', 'GATEWAY_CONFIG')
        - User Task: `participantRole`, `isApproval`, `dueDuration`
        - Service Task: `templateId`, `subject`, `retryCount`, `priority`
        - Gateway: `defaultNextActivityId`, `conditions`

        ### 4. Response Guidelines
        - Provide 2-3 high-quality suggestions.
        - Ensure `reason` clearly explains WHY this step is needed.
        - Generate ONLY the raw JSON. No markdown formatting.
    """)
    @UserMessage("""
        Analyze this graph and suggest next steps.
        
        [Prompt]
        {{prompt}}
        
        [Current Graph Context]
        {{currentGraphJson}}
        
        [Focus Node]
        {{focusNodeId}}
    """)
    SuggestionResponse suggestNextSteps(
            @V("prompt") String prompt,
            @V("currentGraphJson") String currentGraphJson,
            @V("focusNodeId") String focusNodeId
    );
}