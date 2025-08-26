package com.example.campung.global.enums;

import lombok.Getter;

@Getter
public enum LandmarkCategory {
    LIBRARY("도서관", 300),
    RESTAURANT("식당", 300),
    CAFE("카페", 200),
    DORMITORY("기숙사", 500),
    FOOD_TRUCK("푸드트럭", 150),
    EVENT("행사", 200),
    UNIVERSITY_BUILDING("대학건물", 400);

    private final String description;
    private final int defaultRadius; // 기본 검색 반경(미터)

    LandmarkCategory(String description, int defaultRadius) {
        this.description = description;
        this.defaultRadius = defaultRadius;
    }
}