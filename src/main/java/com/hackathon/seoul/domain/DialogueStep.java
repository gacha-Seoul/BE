package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.util.Arrays;
import java.util.List;

/**
 * 대화 1단계 = 플레이어 대사(하단 패널) + NPC 응답(상단 말풍선).
 * 플레이어가 user 대사를 말하면(같은 의미면 인정) NPC가 npc 대사로 답하고 다음 단계로.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"quest_id", "step_no"}))
public class DialogueStep {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    private int stepNo;

    private String target;            // 플레이어가 말하는 상대 (친구, 직원 ...)

    @Column(length = 500) private String userKo;
    @Column(length = 500) private String userRoman;
    @Column(length = 500) private String userEn;

    @Column(length = 1000)
    private String accepts;           // 같이 인정하는 표현 '|' 구분

    private String speaker;           // 응답하는 NPC 이름
    @Column(length = 500) private String npcKo;
    @Column(length = 500) private String npcRoman;
    @Column(length = 500) private String npcEn;

    @Column(length = 1000)
    private String effects;           // 이 단계 통과 시 화면 처리 ';' 구분

    private String requiredEvent;     // 이 단계 전에 필요한 화면 이벤트 (예: selected)

    @Column(length = 500)
    private String retryKo;           // requiredEvent 없을 때 NPC 대사 (예: "어떤 거요?")

    protected DialogueStep() {}

    public List<String> getAcceptList() {
        if (accepts == null || accepts.isBlank()) return List.of();
        return Arrays.stream(accepts.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> getEffectList() { return Quest.Effects.split(effects); }

    public Long getId() { return id; }
    public Quest getQuest() { return quest; }
    public int getStepNo() { return stepNo; }
    public String getTarget() { return target; }
    public String getUserKo() { return userKo; }
    public String getUserRoman() { return userRoman; }
    public String getUserEn() { return userEn; }
    public String getSpeaker() { return speaker; }
    public String getNpcKo() { return npcKo; }
    public String getNpcRoman() { return npcRoman; }
    public String getNpcEn() { return npcEn; }
    public String getRequiredEvent() { return requiredEvent; }
    public String getRetryKo() { return retryKo; }
}
