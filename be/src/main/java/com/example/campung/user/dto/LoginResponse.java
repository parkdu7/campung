package com.example.campung.user.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private String accessToken;
    
    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public LoginResponse(boolean success, String message, String accessToken) {
        this.success = success;
        this.message = message;
        this.accessToken = accessToken;
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
}