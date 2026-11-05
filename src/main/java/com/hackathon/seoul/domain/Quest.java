package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

/**
 * 장소 안의 순차 퀘스트. NPC, 완료 규칙, 보상을 data.sql로 정의한다.
 */
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stage_id", "order_no"}),
    @UniqueConstraint(columnNames = {"stage_id", "code"})
})
@org.hibernate.annotations.Check(constraints = "reward_coin >= 0")
public class Quest {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private int orderNo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompletionRule completionRule;
    public enum CompletionRule { ALL_MISSIONS, FIND_STORE, ACCEPT_SAMPLE, CONFIRM_NEGOTIATION, CONFIRM_ORDER }

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

    @Column(nullable = false)
    private int rewardCoin;

    protected Quest() {}

    public List<String> getMissionList() { return split(missions); }

    public List<String> getStarterSuggestionList() { return split(starterSuggestions); }

    private static List<String> split(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("\\|")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    public Stage getStage() { return stage; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public CompletionRule getCompletionRule() { return completionRule; }
    public Long getId() { return id; }
    public Region getRegion() { return stage.getRegion(); }
    public Category getCategory() { return stage.getCategory(); }
    public int getOrderNo() { return orderNo; }
    public String getPlace() { return stage.getPlace(); }
    public String getNpcRole() { return npcRole; }
    public String getNpcPersona() { return npcPersona; }
    public String getSituation() { return situation; }
    public String getOpeningLine() { return openingLine; }
    public int getRewardCoin() { return rewardCoin; }
}
