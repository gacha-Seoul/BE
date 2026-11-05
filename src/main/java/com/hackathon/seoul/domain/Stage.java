package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

/**
 * 지역 안의 분야별 체험 스테이지.
 * 대화 엔진은 하나, 스테이지는 데이터(data.sql)로만 추가한다.
 */
@Entity
public class Stage {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private int orderNo;

    private String place;           // 영천시장
    private String npcRole;         // 꽈배기 가게 사장님

    @Column(length = 2000)
    private String npcPersona;      // 말투, 성격

    @Column(length = 2000)
    private String situation;       // 상황 설명

    @Column(length = 1000)
    private String missions;        // '|' 로 구분: 인사하기|가격 물어보기|...

    @Column(length = 500)
    private String openingLine;     // NPC 첫 마디

    @Column(length = 500)
    private String starterSuggestions; // 첫 추천 답변 '|' 구분

    private int rewardCoin;

    protected Stage() {}

    public List<String> getMissionList() { return split(missions); }

    public List<String> getStarterSuggestionList() { return split(starterSuggestions); }

    private static List<String> split(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("\\|")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public Long getId() { return id; }
    public Region getRegion() { return region; }
    public Category getCategory() { return category; }
    public int getOrderNo() { return orderNo; }
    public String getPlace() { return place; }
    public String getNpcRole() { return npcRole; }
    public String getNpcPersona() { return npcPersona; }
    public String getSituation() { return situation; }
    public String getOpeningLine() { return openingLine; }
    public int getRewardCoin() { return rewardCoin; }
}
