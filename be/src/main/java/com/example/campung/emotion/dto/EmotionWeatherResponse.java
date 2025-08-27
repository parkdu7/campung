package com.example.campung.emotion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 감정 날씨 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionWeatherResponse {
    
    @JsonProperty("emotionWeather")
    private String emotionWeather;
    
    @JsonProperty("emotionTemperature")
    private Double emotionTemperature;
    
    @JsonProperty("lastUpdated")
    private String lastUpdated;
}