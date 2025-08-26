package com.example.campung.lankmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "랜드마크 수정 응답")
public class LandmarkUpdateResponse {
    
    @Schema(description = "랜드마크 ID", example = "1")
    private Long id;
    
    @Schema(description = "수정 시간", example = "2025-01-01T11:00:00")
    private LocalDateTime updatedAt;
}