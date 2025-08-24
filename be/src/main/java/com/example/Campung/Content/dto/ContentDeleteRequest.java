package com.example.Campung.Content.Dto;

public class ContentDeleteRequest {
    private String reason; // 삭제 사유 (선택사항)
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}