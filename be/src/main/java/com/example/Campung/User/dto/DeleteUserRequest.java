package com.example.Campung.User.dto;

public class DeleteUserRequest {
    private String userId;
    private String password; // 단순 검증용

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
