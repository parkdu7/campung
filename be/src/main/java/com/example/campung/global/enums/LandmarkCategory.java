package com.example.campung.global.enums;

import lombok.Getter;

@Getter
public enum LandmarkCategory {
    LIBRARY("도서관", 300),
    CAFE("카페", 200), 
    CLASSROOM("강의실", 400),
    SPORTS("운동시설", 800),
    RESTAURANT("식당", 300),
    DORMITORY("기숙사", 500),
    CONVENIENCE("편의시설", 250),
    ETC("기타", 500);

    private final String description;
    private final int defaultRadius; // 기본 검색 반경(미터)

    LandmarkCategory(String description, int defaultRadius) {
        this.description = description;
        this.defaultRadius = defaultRadius;
    }
}