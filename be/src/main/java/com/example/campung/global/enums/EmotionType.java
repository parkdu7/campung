package com.example.campung.global.enums;

/**
 * 감정 분석에 사용되는 감정 유형
 */
public enum EmotionType {
    DEPRESSION("우울함"),
    BRIGHTNESS("밝음"),
    EXCITEMENT("신남"),
    ANGER("화남"),
    SADNESS("슬픔"),
    THRILLED("흥분된");

    private final String koreanName;

    EmotionType(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 한국어 이름으로 EmotionType을 찾는 메서드
     */
    public static EmotionType fromKoreanName(String koreanName) {
        for (EmotionType type : values()) {
            if (type.koreanName.equals(koreanName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("해당하는 감정 유형이 없습니다: " + koreanName);
    }

    /**
     * 긍정적 감정 여부 확인
     */
    public boolean isPositive() {
        return this == BRIGHTNESS || this == EXCITEMENT || this == THRILLED;
    }

    /**
     * 부정적 감정 여부 확인
     */
    public boolean isNegative() {
        return this == DEPRESSION || this == ANGER || this == SADNESS;
    }
}