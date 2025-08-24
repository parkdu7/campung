package com.example.campung.user.dto;

public class LogoutResponse {
    private boolean success;
    private String message;

    public LogoutResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
