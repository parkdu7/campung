package com.example.campung.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class LoginResponse {
    @Schema(description = "성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "로그인에 성공했습니다")
    private String message;
    
    @Schema(description = "액세스 토큰", example = "testuser")
    private String accessToken;
    
    @Schema(description = "사용자 닉네임", example = "테스트유저")
    private String nickname;
    
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public LoginResponse(boolean success, String message, String accessToken) {
        this.success = success;
        this.message = message;
        this.accessToken = accessToken;
    }
    
    public LoginResponse(boolean success, String message, String accessToken, String nickname) {
        this.success = success;
        this.message = message;
        this.accessToken = accessToken;
        this.nickname = nickname;
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
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}