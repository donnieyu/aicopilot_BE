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
        Your goal is to suggest the Next Best Action based on the process context and AVAILABLE DATA.

        ### 1. Analysis & Suggestion Logic
        - Analyze the `currentGraphJson` to understand the flow.
        - **Look at `availableVariables`**: These are the data points collected so far.
        - Suggest actions that CONSUME this data.
          - E.g., If `availableVariables` contains 'ApplicantEmail', suggest 'Send Email' and bind it.
          - E.g., If 'ExpenseAmount' exists, suggest 'Manager Approval' with a condition `amount > 1000`.

        ### 2. Data Binding Syntax (Strict)
        - You MUST use the binding syntax provided in the `availableVariables` list.
        - Format: `#{SourceNodeID.VariableAlias}`
        - Populate the `inputMapping` field in the response.
        - Example: `inputMapping`: { "recipient": "#{node_step_1.ApplicantEmail}", "amount": "#{node_step_1.ExpenseAmount}" }

        ### 3. Output Structure (JSON)
        Return a JSON object with a list of `suggestions`.
        - `type`: 'USER_TASK', 'SERVICE_TASK', 'EXCLUSIVE_GATEWAY'
        - `configuration`:
            - SERVICE_TASK (Email): Set `subject` and `templateId`.
            - EXCLUSIVE_GATEWAY: Define `conditions` using variables (e.g., `{{ node_step_1.ExpenseAmount }} > 1000`).

        ### 4. Guidelines
        - Provide 2-3 distinct options (e.g., one happy path, one exception path, or one automated action).
        - `reason` should mention WHICH data is being used (e.g., "Sends email to the address collected in Step 1").
    """)
    @UserMessage("""
        Analyze the graph and available data to suggest next steps.
        
        [Focus Node]
        {{focusNodeId}}
        
        [Available Variables (Upstream Data)]
        {{availableVariables}}
        
        [Current Graph Context]
        {{currentGraphJson}}
    """)
    SuggestionResponse suggestNextSteps(
            @V("currentGraphJson") String currentGraphJson,
            @V("focusNodeId") String focusNodeId,
            @V("availableVariables") String availableVariables
    );
}