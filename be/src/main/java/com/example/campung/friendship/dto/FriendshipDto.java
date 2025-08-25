package com.example.campung.friendship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "친구 관계 DTO")
public class FriendshipDto {

    @Schema(description = "친구 관계 ID", example = "1")
    private Long friendshipId;

    @Schema(description = "사용자 ID", example = "user123")
    private String userId;

    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "상태", example = "accepted")
    private String status;

    @Schema(description = "생성/친구맺은 시간")
    private LocalDateTime createdAt;
}