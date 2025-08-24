package com.example.Campung.User.Dto;

public class LoginRequest {
    private String userId;
    private String password;
    // ★ 추가: FCM 토큰
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