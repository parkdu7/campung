package com.example.campung.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class TestContentResponse {
    
    @Schema(description = "성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "메시지", example = "테스트 컨텐츠가 성공적으로 생성되었습니다")
    private String message;
    
    @Schema(description = "생성된 컨텐츠 개수", example = "15")
    private int createdCount;
    
    public TestContentResponse() {
    }
    
    public TestContentResponse(boolean success, String message, int createdCount) {
        this.success = success;
        this.message = message;
        this.createdCount = createdCount;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getCreatedCount() {
        return createdCount;
    }
    
    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }
}