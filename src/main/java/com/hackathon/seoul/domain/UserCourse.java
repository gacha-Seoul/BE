package com.hackathon.seoul.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 가챠로 뽑은 코스 (지역·카테고리). 재뽑기 불가 → 같은 코스 중복 없음 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class UserCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private LocalDateTime drawnAt;
    private boolean completed;
    private LocalDateTime completedAt;

    protected UserCourse() {}

    public UserCourse(AppUser user, Course course) {
        this.user = user;
        this.course = course;
        this.drawnAt = LocalDateTime.now();
    }

    public void complete() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public LocalDateTime getDrawnAt() { return drawnAt; }
    public boolean isCompleted() { return completed; }
}
