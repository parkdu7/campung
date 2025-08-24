package com.example.Campung.User.dto;

public class DeleteUserResponse {
    private boolean success;
    private String message;

    public DeleteUserResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
