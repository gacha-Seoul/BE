package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 스테이지별 미션 진행 상황. 클리어 판정은 LLM이 아니라 여기(서버)에서 한다. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "stage_id"}))
public class StageProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    private String completedMissions = "";   // "0,2,3"
    private boolean cleared;

    protected StageProgress() {}

    public StageProgress(AppUser user, Stage stage) {
        this.user = user;
        this.stage = stage;
    }

    public Set<Integer> getCompletedSet() {
        Set<Integer> set = new TreeSet<>();
        if (completedMissions == null || completedMissions.isBlank()) return set;
        for (String s : completedMissions.split(",")) {
            try { set.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) { }
        }
        return set;
    }

    /**
     * 미션 완료 처리. 범위 밖 index는 무시.
     * @return 이번 호출로 처음 클리어되었으면 true
     */
    public boolean markCompleted(Collection<Integer> indexes, int totalMissions) {
        Set<Integer> set = getCompletedSet();
        if (indexes != null) {
            for (Integer i : indexes) {
                if (i != null && i >= 0 && i < totalMissions) set.add(i);
            }
        }
        this.completedMissions = set.stream().map(String::valueOf).collect(Collectors.joining(","));
        if (!cleared && totalMissions > 0 && set.size() >= totalMissions) {
            this.cleared = true;
            return true;
        }
        return false;
    }

    /** 다시 시작할 때: 이미 클리어한 스테이지는 기록 유지, 아니면 미션 초기화 */
    public void resetIfNotCleared() {
        if (!cleared) this.completedMissions = "";
    }

    public Long getId() { return id; }
    public Stage getStage() { return stage; }
    public boolean isCleared() { return cleared; }
}
