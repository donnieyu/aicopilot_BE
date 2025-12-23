package com.example.aicopilot.agent;

import com.example.aicopilot.dto.definition.ProcessDefinition;
import com.example.aicopilot.dto.definition.ProcessStep;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.List;

@AiService
public interface ProcessOutliner {

    @SystemMessage("""
        You are a 'Business Process Analyst'.
        Your goal is to draft a **Structured Process Definition List** from user requirements.
        
        ### Rules
        1. **Identify Key Steps:** List the logical steps from start to finish.
        2. **Detect Decision Points:**
           - If a step involves an approval, review, or condition (e.g., "If amount > 1000"), explicitly mark it as 'DECISION'.
           - **CRITICAL:** For 'DECISION' steps, imply what happens on rejection/failure in the description.
        3. **Role Assignment:** Clearly define who (Person or System) performs each step.
        4. **Completeness:** Ensure the process covers the happy path and major exception paths.
        
        ### Output Constraints (STRICT)
        - Return a JSON object matching the `ProcessDefinition` structure.
        - **`type` Field:** MUST be one of the following exact values:
          - `'ACTION'`: For any task, activity, or process step. (Do NOT use 'Process', 'Task', or anything else).
          - `'DECISION'`: For gateways, approvals, or conditional checks.
        - **VIOLATION of `type` constraint will cause system failure.**
        
        **IMPORTANT:** The output must be valid JSON.
    """)
    ProcessDefinition draftDefinition(String userRequest);

    @UserMessage("""
        Based on the Topic and Description provided by the user, **Draft** a detailed process step list.
        
        [Input]
        Topic: {{topic}}
        Context/Description: {{description}}
        
        [Goal]
        - If the description provides a specific flow (e.g., "A -> B -> C"), **FOLLOW IT STRICTLY**.
        - If the description is vague, use your knowledge of industry standards for the given topic.
        - Break down the process into 3-7 logical steps.
        - **Role Inference:** Infer the most appropriate actor (Role) for each step (e.g., 'Employee', 'Manager', 'System', 'Finance Team').
        - **Decision Points:** If the context implies an approval or check, mark it as 'DECISION'.
        
        [Output Structure & Type Rules]
        Return a JSON with 'topic' and a list of 'steps'. Each step has 'stepId', 'name', 'role', 'description', 'type'.
        **IMPORTANT:** The `type` field MUST be exactly "ACTION" or "DECISION". Never use "Process" or any other value.
        Ensure the response is valid JSON.
    """)
    ProcessDefinition suggestSteps(
            @V("topic") String topic,
            @V("description") String description
    );

    @UserMessage("""
        Suggest a **SINGLE** process step to be inserted at the specified index within the current workflow.
        
        [Context]
        Topic: {{topic}}
        Overall Goal: {{context}}
        Insertion Index: {{stepIndex}} (The new step will be placed here)
        
        [Current Process Flow]
        {{currentSteps}}
        
        [Goal]
        - Analyze the steps BEFORE and AFTER the insertion index.
        - Suggest a **missing link** or a **logical bridge** between them.
        - If inserting at the end, suggest a logical conclusion or next action.
        - If inserting at the beginning, suggest a preparation or initiation step.
        - **Consistency:** Ensure the role and action type fit the surrounding context.
        
        [Output Requirement]
        Return a **SINGLE JSON object** matching `ProcessStep` structure:
        - `stepId`: Generate a temporary ID.
        - `name`: Concise title.
        - `role`: The actor for this step.
        - `description`: Detailed action description explaining why this step is needed here.
        - `type`: Must be 'ACTION' or 'DECISION'. Do NOT use 'Process'.
        
        Provide the response in JSON format.
    """)
    ProcessStep suggestSingleStep(
            @V("topic") String topic,
            @V("context") String context,
            @V("stepIndex") int stepIndex,
            @V("currentSteps") List<String> currentSteps
    );
}