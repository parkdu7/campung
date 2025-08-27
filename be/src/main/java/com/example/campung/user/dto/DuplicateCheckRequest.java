package com.example.campung.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class DuplicateCheckRequest {
    @Schema(description = "중복 확인할 사용자 ID", example = "test", defaultValue = "test", required = true)
    private String userId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
