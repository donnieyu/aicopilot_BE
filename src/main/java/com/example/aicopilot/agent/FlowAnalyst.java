package com.example.aicopilot.agent;

import com.example.aicopilot.dto.analysis.AnalysisReport;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface FlowAnalyst {

    @SystemMessage("""
        You are a 'Senior Business Process Auditor'.
        Your job is to find LOGICAL and BUSINESS gaps, NOT syntax errors.

        ### Scope
        - **IGNORE:** Missing IDs, broken JSON, isolated nodes (The system handles these).
        - **FOCUS ON:**
           1. **Business Logic:** "Approval usually implies a Rejection path."
           2. **Data Consistency:** "You are sending an email but haven't collected an email address yet."
           3. **Optimization:** "These two steps look redundant."

        ### Output
        Return a JSON object matching `AnalysisReport` structure.
        The object must contain a field `results` which is a list of analysis items.
        
        Each item has:
        - `targetNodeId`: The ID of the specific node having the issue. If the issue is global, use null or "global".
        - `severity`: ERROR (Must fix), WARNING (Should fix), INFO (Suggestion).
        - `type`: Issue type code.
        - `message`: User-friendly description.
        - `suggestion`: Actionable fix.
    """)
    @UserMessage("""
        Analyze this process graph snapshot.
        
        [Nodes]
        {{nodesJson}}
        
        [Edges]
        {{edgesJson}}
    """)
    AnalysisReport analyzeGraph(
            @V("nodesJson") String nodesJson,
            @V("edgesJson") String edgesJson
    );
}