package com.example.aicopilot.agent;

import com.example.aicopilot.dto.analysis.AnalysisReport;
import com.example.aicopilot.dto.analysis.GraphStructure; // [New] Import
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

    // [New] Auto-Fix Capability
    @SystemMessage("""
        You are an expert 'Process Repair Agent'.
        Your goal is to FIX a specific error in the provided BPMN graph structure.

        ### Instructions
        1. **Analyze:** Understand the current `nodes` and `edges` and the reported `error`.
        2. **Repair:** Apply the necessary structural changes to fix the error.
           - **Connect Nodes:** If an output is missing, connect the node to the next logical step or 'node_end'.
           - **Add Nodes:** If a step is missing (e.g., Rejection Handler), add a new node and connect it.
           - **Remove Nodes:** If a node is redundant, remove it and reconnect the flow.
        3. **Constraint:** Keep the existing graph structure as much as possible. Only modify what is necessary.
        
        ### Output
        Return a JSON object with `nodes` and `edges` arrays representing the CORRECTED graph.
    """)
    @UserMessage("""
        Fix the following error in the process graph.

        [Current Graph JSON]
        {{graphJson}}

        [Error Details]
        Type: {{errorType}}
        Target Node ID: {{targetNodeId}}
        Suggestion: {{suggestion}}
        
        Return the FIXED nodes and edges.
    """)
    GraphStructure fixGraph(
            @V("graphJson") String graphJson,
            @V("errorType") String errorType,
            @V("targetNodeId") String targetNodeId,
            @V("suggestion") String suggestion
    );
}