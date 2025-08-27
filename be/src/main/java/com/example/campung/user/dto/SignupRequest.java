package com.example.campung.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class SignupRequest {
    @Schema(description = "사용자 ID", example = "test", defaultValue = "test", required = true)
    private String userId;
    
    @Schema(description = "비밀번호", example = "test", defaultValue = "test", required = true)
    private String password;
    
    @Schema(description = "닉네임", example = "test", defaultValue = "test", required = true)
    private String nickname;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
