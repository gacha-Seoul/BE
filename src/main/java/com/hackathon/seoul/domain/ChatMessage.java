package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ChatMessage {

    public enum Role { USER, NPC }

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
    private Role role;

    @Column(length = 2000)
    private String content;

    private LocalDateTime createdAt;

    protected ChatMessage() {}

    public ChatMessage(AppUser user, Quest quest, Role role, String content) {
        this.user = user;
        this.quest = quest;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getContent() { return content; }
}
