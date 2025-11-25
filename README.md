# 🏛️ AI-Native Workflow Architect

**"More than just a generator—a true AI Co-Architect."**

This project is an intelligent backend service that transforms natural language requirements into executable business processes, data models, and user interfaces. Design complex enterprise application structures with a single prompt, and collaborate in real-time to refine and evolve them.

## 🚀 Key Features

### 1. **⚡ Event-Driven Incremental Generation**

No more waiting. Visual results for the process skeleton are provided immediately (approx. 20 seconds) after completion, while data and forms are generated asynchronously in the background.
- **Process First**: Instant visualization of the workflow structure.
- **Optimistic UI**: Real-time "Analyzing Data..." status updates, with automatic completion notifications.

### 2. **🧠 Atomic Data & Lineage**

Say goodbye to vague data like "Leave Application Form." The AI imagines the actual UI to decompose data into atomic fields such as ```StartDate``` and ```LeaveType```.

- **Atomic Explosion**: Precise data modeling at the field level, not the task level.
- **Source Tracking**: Every data variable remembers the node (```sourceNodeId```) where it was created.

### 3. **🤖 Hybrid Suggestion Engine**

The AI doesn't just build blindly. When you select a node, it analyzes the current context to suggest the "Next Best Action."

- **Smart Binding**: Suggests actions like "Send Email" with the ```applicantEmail``` variable from the previous step automatically mapped (```#{node.email}```).
- **Explicit Gateway**: Automatically generates branching logic (Gateway) after approval nodes.

### 4. **🛡️ Type Safety & Polymorphism**

- **Unified Configuration**: Manages settings for diverse nodes like ```UserTask```, ```ServiceTask```, and ```Gateway``` under a single unified structure (```NodeConfiguration```), maximizing frontend interoperability.

## 🏗️ Architecture

This system follows a **3-Stage Event Pipeline** structure based on **Spring Boot** and **LangChain4j**.

```
graph TD
    Client[Frontend Client] -->|POST /start| Controller
    Controller -->|Async| Orchestrator[Workflow Orchestrator]
    
    subgraph "Stage 1: Structure"
        Orchestrator -->|Call| PA[Process Architect Agent]
        PA -->|JSON| Process[Process Definition]
    end
    
    Process -->|Event Pub| EventBus[Spring Event Bus]
    
    subgraph "Stage 2 & 3: Detail (Async)"
        EventBus -->|Listener| AsyncGen[Async Artifact Generator]
        AsyncGen -->|Call| DM[Data Modeler Agent]
        DM -->|JSON| Data[Data Entities]
        AsyncGen -->|Call| FD[Form UX Designer Agent]
        FD -->|JSON| Form[Form Schema]
    end
    
    AsyncGen -->|Update| Repo[Job Repository]
    Client -->|Polling| Repo
```

### **🛠️ Tech Stack**
- **Core**: Java 17, Spring Boot 3.x
- **AI**: LangChain4j, OpenAI (GPT-4o-mini optimized)
- **Data**: In-Memory ConcurrentHashMap (Prototype)
- **API**: RESTful API (Polling & On-Demand Suggestion)

## **🔌 API Guide (Quick Start)**
### **1. Start Process Generation**
```POST /api/copilot/start```
```json
{
   "userPrompt": "Create an expense claim process where claims over $1,000 require manager approval, otherwise auto-approved."
}
```

### **2. Check Progress (Polling)**

```GET /api/copilot/status/{jobId}```

* Response Example (In Progress):
```json
{
    "state": "PROCESSING",
    "lastUpdatedStage": "PROCESS",
    "stageDurations": { "PROCESS": 21000 },
    "totalElapsedMillis": 21500
}
```

### **3. Request Real-time Suggestion (On-Demand)**

```POST /api/copilot/suggest```
* Select a specific node (```focusNodeId```) to get recommendations for the next action.

## **📂 Project Structure**

* ```agent/```: AI Persona definitions (ProcessArchitect, DataModeler, etc.)
* ```dto/```: Strongly typed data models agreed upon with the frontend (NodeConfiguration, etc.)
* ```event/```: Spring Event classes for asynchronous processing
* ```service/```: Business logic and orchestration (WorkflowOrchestrator, AsyncArtifactGenerator)

## **📝 Developer Notes**

* **Speed Optimization**: AI model ```temperature``` set to ```0.0``` for faster and more consistent responses.
* **Frontend Integration**: Use the ```configType``` field in ```NodeConfiguration``` to dynamically render UI components.
