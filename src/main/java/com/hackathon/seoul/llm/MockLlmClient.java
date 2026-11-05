package com.hackathon.seoul.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.seoul.domain.Quest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** 키 없는 연동 테스트용 고정 시나리오. 실제 의미 판정은 real 모드에서 수행한다. */
@Component
@ConditionalOnProperty(name = "llm.mock", havingValue = "true", matchIfMissing = true)
public class MockLlmClient implements LlmClient {
    private final JsonNode fixtures;
    public MockLlmClient(ObjectMapper json) {
        try (var stream = new ClassPathResource("prompts/mock-responses.txt").getInputStream()) {
            fixtures = json.readTree(stream);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Override
    public String completeJson(LlmPurpose purpose, String system, List<LlmMessage> messages) {
        if (purpose == LlmPurpose.PHRASE_CARD) return fixtures.get("PHRASE_CARD").toString();
        String rule = Quest.CompletionRule.ALL_MISSIONS.name();
        for (Quest.CompletionRule value : Quest.CompletionRule.values())
            if (system.contains("[완료 규칙] " + value.name())) rule = value.name();
        JsonNode turns = fixtures.get(rule);
        int turn = (int) messages.stream().filter(x -> "user".equals(x.role())).count() - 1;
        return turns.get(Math.max(0, Math.min(turn, turns.size() - 1))).toString();
    }
}
