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
    UNIVERSITY_BUILDING("대학건물", 400),
    HACKATHON_ZONE("해커톤 ZONE", 100),
    ACCOMMODATION("숙소", 300),
    CONVENIENCE_STORE("편의점", 20),
    SMOKING_AREA("흡연구역", 5),
    COFFEE_ZONE("커피 ZONE", 10),
    PHOTO_SPOT("사진 스팟", 150);

    private final String description;
    private final int defaultRadius; // 기본 검색 반경(미터)

    LandmarkCategory(String description, int defaultRadius) {
        this.description = description;
        this.defaultRadius = defaultRadius;
    }
}