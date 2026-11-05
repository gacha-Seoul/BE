package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

/** 장소 안의 퀘스트 Q1~Q4 (순차 해금). 대화는 DialogueStep 으로 단계별 저장. */
@Entity
public class Quest {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    private int orderNo;
    private String code;              // Q1
    private String name;              // 목록용 짧은 이름: 길 찾기
    private String title;             // 사진 속 전집 찾기

    @Column(length = 1000)
    private String story;             // 상황 줄거리 (시작 안내 말풍선)

    private String location;          // 진행 위치 설명
    private String destinationKey;    // 3D 목적지 테두리 키 (jeonjip, pub ...)

    @Column(length = 1000)
    private String npcPersona;        // NPC 말투 (AI 예외 대사용)

    private String openingSpeaker;    // 대화 시작 시 먼저 말하는 NPC (없으면 null)
    @Column(length = 500) private String openingKo;
    @Column(length = 500) private String openingRoman;
    @Column(length = 500) private String openingEn;

    @Column(length = 2000)
    private String rules;             // 예외 처리 규칙 (AI 참고)

    private String completeEvent;     // 마지막 대화 후 필요한 화면 이벤트 (예: arrived). 없으면 null

    @Column(length = 1000)
    private String completeEffects;   // 완료 시 화면 처리, ';' 구분

    private int rewardCoin;

    protected Quest() {}

    public List<String> getCompleteEffectList() { return Effects.split(completeEffects); }

    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public int getOrderNo() { return orderNo; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public String getStory() { return story; }
    public String getLocation() { return location; }
    public String getDestinationKey() { return destinationKey; }
    public String getNpcPersona() { return npcPersona; }
    public String getOpeningSpeaker() { return openingSpeaker; }
    public String getOpeningKo() { return openingKo; }
    public String getOpeningRoman() { return openingRoman; }
    public String getOpeningEn() { return openingEn; }
    public String getRules() { return rules; }
    public String getCompleteEvent() { return completeEvent; }
    public int getRewardCoin() { return rewardCoin; }

    /** 효과 문자열 "TYPE:value;TYPE:value" 분리 */
    public static final class Effects {
        private Effects() {}
        public static List<String> split(String s) {
            if (s == null || s.isBlank()) return List.of();
            return Arrays.stream(s.split(";")).map(String::trim).filter(x -> !x.isEmpty()).toList();
        }
    }
}
