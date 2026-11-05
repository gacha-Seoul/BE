package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 가챠로 해금한 지역 + 정복 여부 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "region_id"}))
public class UserRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    private boolean conquered;
    private LocalDateTime unlockedAt;
    private LocalDateTime conqueredAt;

    protected UserRegion() {}

    public UserRegion(AppUser user, Region region) {
        this.user = user;
        this.region = region;
        this.unlockedAt = LocalDateTime.now();
    }

    public void conquer() {
        this.conquered = true;
        this.conqueredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Region getRegion() { return region; }
    public boolean isConquered() { return conquered; }
}
