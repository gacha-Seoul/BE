package com.hackathon.seoul.domain;

import jakarta.persistence.*;

/** 가챠 후보 카드 = 지역 · 카테고리 조합 (예: 서대문구 · 문화). 장소 4개를 가진다. */
@Entity
public class Course {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private int orderNo;            // 후보 카드 순서

    @Column(length = 500)
    private String openCopy;        // 뽑힌 뒤 캐릭터 대사: "서대문구의 문화 퀘스트가 열렸어. 첫 장소를 확인해 봐"

    private String imageKey;        // 후보 카드 대표 이미지

    protected Course() {}

    public Long getId() { return id; }
    public Region getRegion() { return region; }
    public Category getCategory() { return category; }
    public int getOrderNo() { return orderNo; }
    public String getOpenCopy() { return openCopy; }
    public String getImageKey() { return imageKey; }
}
