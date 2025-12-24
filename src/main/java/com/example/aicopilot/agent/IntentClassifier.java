package com.example.aicopilot.agent;

import com.example.aicopilot.dto.chat.IntentResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 인텐트 분류 에이전트.
 * OpenAI JSON 모드 요구사항에 맞춰 "json" 키워드를 포함하고 객체 구조를 반환합니다.
 */
@AiService
public interface IntentClassifier {

    @SystemMessage("""
        You are an 'Intent Classifier Agent' specialized in business architecture.
        Your task is to categorize the user's input into one of the following 5 intents.

        1. **DESIGN**: Creating a new process map or workflow from scratch.
        2. **MODIFY**: Updating, adding, or deleting nodes in an existing canvas.
        3. **ANALYZE**: Performing logic audits or seeking optimization advice.
        4. **GUIDE**: Questions about app features or usage instructions.
        5. **CHAT**: General business conversation within the design domain.

        ### Output Requirement
        - You MUST return the result in **JSON** format.
        - Use the key 'intent' for the classification value.
        - Values MUST be one of: DESIGN, MODIFY, ANALYZE, GUIDE, CHAT.
        """)
    IntentResponse classify(@UserMessage String userQuery);
}