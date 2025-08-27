package com.example.campung.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginRequest {
    @Schema(description = "사용자 ID", example = "test", defaultValue = "test", required = true)
    private String userId;
    
    @Schema(description = "비밀번호", example = "test", defaultValue = "test", required = true)
    private String password;
    
    @Schema(description = "FCM 토큰", example = "fcm_token_example_123")
    private String fcmToken;
    
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
}