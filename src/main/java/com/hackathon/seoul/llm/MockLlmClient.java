package com.hackathon.seoul.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * API 키 없이 프론트 연동/데모 리허설용.
 * 사용자가 한 번 말할 때마다 미션을 하나씩 완료 처리한다.
 * 실제 데모 직전에 LLM_MOCK=false 로 바꾸면 진짜 AI로 전환.
 */
@Component
@ConditionalOnProperty(name = "llm.mock", havingValue = "true", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private static final String[] REPLIES = {
            "아이고, 반가워요! 뭐 찾아요?",
            "그거 하나에 천오백 원이에요~ 몇 개 줄까?",
            "좋아요! 금방 싸 줄게요. 카드? 현금?",
            "고마워요~ 또 와요!"
    };

    @Override
    public String completeJson(LlmPurpose purpose, String systemPrompt, List<LlmMessage> messages) {
        if (purpose == LlmPurpose.PHRASE_CARD) {
            return """
                    {"title":"오늘 배운 표현","phrases":[
                      {"korean":"안녕하세요","romanization":"annyeonghaseyo","meaning":"Hello","whenToUse":"가게에 들어갈 때"},
                      {"korean":"이거 얼마예요?","romanization":"igeo eolmayeyo?","meaning":"How much is this?","whenToUse":"가격을 물을 때"},
                      {"korean":"두 개 주세요","romanization":"du gae juseyo","meaning":"Two, please","whenToUse":"주문할 때"},
                      {"korean":"카드 돼요?","romanization":"kadeu dwaeyo?","meaning":"Do you take cards?","whenToUse":"결제할 때"},
                      {"korean":"감사합니다","romanization":"gamsahamnida","meaning":"Thank you","whenToUse":"나갈 때"}
                    ]}""";
        }
        long userTurns = messages.stream().filter(m -> "user".equals(m.role())).count();
        int idx = (int) Math.max(0, userTurns - 1);
        String reply = REPLIES[Math.min(idx, REPLIES.length - 1)];
        String correction = idx == 1
                ? "{\"original\":\"이거 얼마\",\"better\":\"이거 얼마예요?\",\"reason\":\"Add '-예요' to sound polite.\"}"
                : "null";
        return """
                {"npcReply":"%s",
                 "correction":%s,
                 "culturalTip":"In traditional markets, friendly small talk is welcome.",
                 "suggestions":[
                   {"korean":"이거 얼마예요?","romanization":"igeo eolmayeyo?","meaning":"How much is this?"},
                   {"korean":"두 개 주세요","romanization":"du gae juseyo","meaning":"Two, please"},
                   {"korean":"카드 돼요?","romanization":"kadeu dwaeyo?","meaning":"Do you take cards?"}],
                 "newlyCompletedMissions":[%d]}
                """.formatted(reply, correction, idx);
    }
}
