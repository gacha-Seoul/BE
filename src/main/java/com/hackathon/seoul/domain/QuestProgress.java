package com.hackathon.seoul.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id"}))
public class QuestProgress {
    public enum Status { IN_PROGRESS, COMPLETED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false) private Quest quest;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Status status = Status.IN_PROGRESS;
    @Column(nullable = false, columnDefinition = "json") private String contextJson = "{}";
    @Column(nullable = false) private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    protected QuestProgress() {}
    public QuestProgress(AppUser user, Quest quest) {
        this.user = user; this.quest = quest; this.startedAt = LocalDateTime.now();
    }
    public void restart() {
        if (!isCompleted()) { contextJson = "{}"; startedAt = LocalDateTime.now(); }
    }
    public boolean complete() {
        if (isCompleted()) return false;
        status = Status.COMPLETED; completedAt = LocalDateTime.now(); return true;
    }
    public void updateContext(String contextJson) { if (!isCompleted()) this.contextJson = contextJson; }
    public Long getId() { return id; }
    public Quest getQuest() { return quest; }
    public boolean isCompleted() { return status == Status.COMPLETED; }
    public Status getStatus() { return status; }
    public String getContextJson() { return contextJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
