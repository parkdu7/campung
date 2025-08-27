package com.example.campung.global.enums;

/**
 * 감정 날씨 유형
 */
public enum WeatherType {
    SUNNY("맑음", "☀️"),
    PARTLY_CLOUDY("구름 조금", "⛅"),
    CLOUDY("흐림", "☁️"),
    MOSTLY_CLOUDY("구름 많음", "☁️"),
    RAINY("비", "🌧️");

    private final String koreanName;
    private final String emoji;

    WeatherType(String koreanName, String emoji) {
        this.koreanName = koreanName;
        this.emoji = emoji;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEmoji() {
        return emoji;
    }

    /**
     * 한국어 이름으로 WeatherType을 찾는 메서드
     */
    public static WeatherType fromKoreanName(String koreanName) {
        for (WeatherType type : values()) {
            if (type.koreanName.equals(koreanName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("해당하는 날씨 유형이 없습니다: " + koreanName);
    }
}