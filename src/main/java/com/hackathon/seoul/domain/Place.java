package com.hackathon.seoul.domain;

import jakarta.persistence.*;

/** 코스 안의 장소 4개 (전통시장, 대학가, 뷰티스토어, 느좋카페). 순차 해금. */
@Entity
public class Place {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private int orderNo;

    private String name;              // 전통시장
    private String representative;    // 대표 장소: 영천시장

    @Column(length = 500)
    private String candidates;        // 연결된 실제 장소: 영천시장, 포방터시장, ...

    private String pinKey;            // 지역 지도 핀 위치 키 (프론트 리소스)
    private String imageKey;

    private String title;             // 시작 화면 제목

    @Column(length = 2000)
    private String story;             // 시작 화면 캐릭터 말풍선

    @Column(length = 1000)
    private String endingText;        // 장소 완료 시 (없으면 null)

    protected Place() {}

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public int getOrderNo() { return orderNo; }
    public String getName() { return name; }
    public String getRepresentative() { return representative; }
    public String getCandidates() { return candidates; }
    public String getPinKey() { return pinKey; }
    public String getImageKey() { return imageKey; }
    public String getTitle() { return title; }
    public String getStory() { return story; }
    public String getEndingText() { return endingText; }
}
