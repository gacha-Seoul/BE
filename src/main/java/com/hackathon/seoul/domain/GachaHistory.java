package com.hackathon.seoul.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_id"}))
@org.hibernate.annotations.Check(constraints = "cost >= 0 and coins_left >= 0")
public class GachaHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false) private Region region;
    @Column(nullable = false) private int cost;
    @Column(nullable = false, length = 100) private String requestId;
    @Column(nullable = false) private int coinsLeft;
    @Column(nullable = false) private LocalDateTime createdAt;
    protected GachaHistory() {}
    public GachaHistory(AppUser user, Region region, int cost, String requestId) {
        this.user = user; this.region = region; this.cost = cost; this.requestId = requestId;
        this.coinsLeft = user.getCoins(); this.createdAt = LocalDateTime.now();
    }
    public Region getRegion() { return region; }
    public int getCoinsLeft() { return coinsLeft; }
    public String getRequestId() { return requestId; }
}
