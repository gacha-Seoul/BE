package com.hackathon.seoul.domain;

import jakarta.persistence.*;

/** 지역·분야에 속하는 장소. 대화와 보상은 하위 퀘스트에 정의한다. */
@Entity
public class Stage {
    @Id private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Category category;
    @Column(nullable = false) private int orderNo;
    @Column(nullable = false) private String place;
    @Column(length = 2000) private String introStory;
    @Column(length = 1000) private String imageUrl;
    protected Stage() {}
    public Long getId() { return id; }
    public Region getRegion() { return region; }
    public Category getCategory() { return category; }
    public int getOrderNo() { return orderNo; }
    public String getPlace() { return place; }
    public String getIntroStory() { return introStory; }
    public String getImageUrl() { return imageUrl; }
}
