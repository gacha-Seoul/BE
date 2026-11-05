package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 유저별 퀘스트 진행. (user, quest) 유니크 → 코인 중복 지급 방지.
 * passedSteps: 통과한 대화 단계 수. 완료(DONE)한 퀘스트는 재수행 불가, 기록 유지.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
public class QuestProgress {

    public enum Status { IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;

    private int passedSteps;
    private String events = "";          // 받은 화면 이벤트 "selected,arrived"
    private LocalDateTime completedAt;

    protected QuestProgress() {}

    public QuestProgress(AppUser user, Quest quest) {
        this.user = user;
        this.quest = quest;
    }

    public Set<String> getEventSet() {
        if (events == null || events.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(events.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void addEvent(String e) {
        Set<String> set = getEventSet();
        set.add(e);
        this.events = String.join(",", set);
    }

    public void passStep() { this.passedSteps++; }

    /** @return 처음 완료면 true (코인 지급 기준) */
    public boolean markDone() {
        if (status == Status.DONE) return false;
        status = Status.DONE;
        completedAt = LocalDateTime.now();
        return true;
    }

    /** 이탈 후 재시작: 완료 안 된 퀘스트만 처음부터 */
    public void resetIfNotDone() {
        if (status != Status.DONE) {
            passedSteps = 0;
            events = "";
        }
    }

    public boolean isDone() { return status == Status.DONE; }
    public int getPassedSteps() { return passedSteps; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Long getId() { return id; }
    public Quest getQuest() { return quest; }
}
