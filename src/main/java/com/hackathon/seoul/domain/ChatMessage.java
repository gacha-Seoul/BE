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
    @JoinColumn(name = "stage_id", nullable = false)
    private Stage stage;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(length = 2000)
    private String content;

    private LocalDateTime createdAt;

    protected ChatMessage() {}

    public ChatMessage(AppUser user, Stage stage, Role role, String content) {
        this.user = user;
        this.stage = stage;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getContent() { return content; }
}
