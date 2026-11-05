package com.hackathon.seoul.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GameProperties {
    @Value("${game.initial-coins:0}") public int initialCoins;
    @Value("${game.free-draws:5}") public int freeDraws;
    @Value("${game.draw-cost:1}") public int drawCost;
    /** true: 뽑은 뒤 퀘스트를 1개 이상 완료해야 다음 뽑기 가능 (재뽑기 불가) */
    @Value("${game.draw-requires-quest:true}") public boolean drawRequiresQuest;
    /** 첫 뽑기 고정: "구코드-카테고리" (예: SEODAEMUN-CULTURE). 비우면 완전 랜덤 */
    @Value("${game.first-course:}") public String firstCourse;
}
