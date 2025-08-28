package com.example.campung.test.dto;

import com.example.campung.global.enums.PostType;
import com.example.campung.global.enums.EmotionTestType;
import io.swagger.v3.oas.annotations.media.Schema;

public class TestContentRequest {
    
    @Schema(description = "위도", example = "36.1077", required = true)
    private Double latitude;
    
    @Schema(description = "경도", example = "128.419", required = true)
    private Double longitude;
    
    @Schema(description = "게시글 카테고리", example = "FREE", required = true)
    private PostType category;
    
    @Schema(description = "사용자 ID", example = "test", required = true)
    private String userId;
    
    @Schema(description = "감정 테스트 타입", 
            example = "BRIGHT", 
            required = true,
            allowableValues = {"DEPRESS", "BRIGHT", "NON_EMOTION_INFO", "ANGRY", "EXCITE", "RANDOM", "ALL_MIXED"})
    private EmotionTestType emotionType;
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public PostType getCategory() {
        return category;
    }
    
    public void setCategory(PostType category) {
        this.category = category;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public EmotionTestType getEmotionType() {
        return emotionType;
    }
    
    public void setEmotionType(EmotionTestType emotionType) {
        this.emotionType = emotionType;
    }
}