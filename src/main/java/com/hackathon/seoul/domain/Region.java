package com.hackathon.seoul.domain;

import jakarta.persistence.*;

/** 가챠로 뽑는 단위 = 서울의 구 */
@Entity
public class Region {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;        // SEODAEMUN (3D 지도와 매칭할 키)

    @Column(nullable = false)
    private String name;        // 서대문구 (GeoJSON의 구 이름과 동일하게)

    private String nameEn;      // Seodaemun-gu

    @Column(length = 1000)
    private String description;

    protected Region() {}

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNameEn() { return nameEn; }
    public String getDescription() { return description; }
}
