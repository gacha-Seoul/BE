package com.hackathon.seoul.llm;

import java.util.List;

/** JSON 문자열을 돌려주는 LLM 클라이언트. 구현체: OpenAiLlmClient(실제), MockLlmClient(키 없이 개발용) */
public interface LlmClient {
    String completeJson(LlmPurpose purpose, String systemPrompt, List<LlmMessage> messages);
}
