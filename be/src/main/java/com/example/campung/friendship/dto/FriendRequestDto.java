package com.example.campung.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "친구 요청 DTO")
public class FriendRequestDto {

    @Schema(description = "요청자 ID (헤더에서 자동 설정)", example = "1", hidden = true)
    private String requesterId;

    @NotBlank
    @Schema(description = "대상 사용자 ID", example = "target123", required = true)
    private String targetUserId;
}