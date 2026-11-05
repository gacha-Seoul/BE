package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = @Index(name = "idx_message_progress_id", columnList = "quest_progress_id,id"))
public class ChatMessage {

    public enum Role { USER, NPC }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_progress_id", nullable = false)
    private QuestProgress progress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {}

    public ChatMessage(QuestProgress progress, Role role, String content) {
        this.progress = progress;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getContent() { return content; }
}
