package com.example.campung.emotion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 감정 분석 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionStatisticsResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private EmotionStatisticsData data;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionStatisticsData {
        
        @JsonProperty("averageScores")
        private Map<String, Double> averageScores;
        
        @JsonProperty("emotionWeather")
        private String emotionWeather;
        
        @JsonProperty("emotionTemperature")
        private Double emotionTemperature;
        
        @JsonProperty("lastUpdated")
        private String lastUpdated;
        
        @JsonProperty("totalAnalyzedPosts")
        private Integer totalAnalyzedPosts;
        
        @JsonProperty("hourlyPostsCount")
        private Map<String, Integer> hourlyPostsCount;
    }
}