package com.hackathon.seoul.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * API 키 없이 프론트 연동/리허설용 (LLM_MOCK=true).
 * 퀘스트 대화는 QuestService가 "말할 때마다 다음 조건 1개 달성"으로 처리한다.
 * 여기서는 표현 카드만 고정 응답을 준다.
 */
@Component
@ConditionalOnProperty(name = "llm.mock", havingValue = "true", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public boolean isMock() { return true; }

    @Override
    public String completeJson(LlmPurpose purpose, String systemPrompt, List<LlmMessage> messages) {
        if (purpose == LlmPurpose.PHRASE_CARD) {
            return """
                    {"title":"시장에서 쓸 표현","phrases":[
                      {"korean":"전집이 어디예요?","romanization":"jeonjibi eodiyeyo?","meaning":"Where is the jeon shop?","whenToUse":"가게 위치를 물을 때"},
                      {"korean":"뭐가 맛있어요?","romanization":"mwoga masisseoyo?","meaning":"What's good here?","whenToUse":"추천받을 때"},
                      {"korean":"조금 깎아 주세요.","romanization":"jogeum kkakka juseyo.","meaning":"A little discount, please.","whenToUse":"흥정할 때"},
                      {"korean":"순대만 주세요.","romanization":"sundaeman juseyo.","meaning":"Just sundae, please.","whenToUse":"간을 빼고 싶을 때"},
                      {"korean":"포장해 주세요.","romanization":"pojanghae juseyo.","meaning":"To go, please.","whenToUse":"포장할 때"}
                    ]}""";
        }
        return "{\"newlySatisfiedSlots\":[],\"npcReply\":null,\"correction\":null}";
    }
}
