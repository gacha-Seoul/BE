package com.hackathon.seoul.domain;

/** 주최 측 소재의 5개 분야 = 지역별 5개 스테이지 */
public enum Category {
    HISTORY("역사"),
    CULTURE("문화"),
    ART("예술"),
    TOURISM("관광"),
    LIVING("생활자원");

    private final String label;

    Category(String label) { this.label = label; }

    public String getLabel() { return label; }
}
