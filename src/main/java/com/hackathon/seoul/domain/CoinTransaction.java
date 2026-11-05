package com.hackathon.seoul.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class CoinTransaction {
    public enum Type { INITIAL, QUEST_CLEAR, REGION_CONQUER, GACHA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Column(nullable = false) private int amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Type type;
    @Column(nullable = false, unique = true, length = 200) private String eventKey;
    @Column(nullable = false) private LocalDateTime createdAt;
    protected CoinTransaction() {}
    public CoinTransaction(AppUser user, int amount, Type type, String eventKey) {
        this.user = user; this.amount = amount; this.type = type;
        this.eventKey = eventKey; this.createdAt = LocalDateTime.now();
    }
    public int getAmount() { return amount; }
    public String getEventKey() { return eventKey; }
    public Type getType() { return type; }
}
