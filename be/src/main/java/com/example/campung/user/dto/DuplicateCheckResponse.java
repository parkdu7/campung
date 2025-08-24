package com.example.campung.user.dto;

public class DuplicateCheckResponse {
    private boolean success;
    private String message;
    private boolean available; // true = 사용 가능, false = 중복

    public DuplicateCheckResponse(boolean success, String message, boolean available) {
        this.success = success;
        this.message = message;
        this.available = available;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
