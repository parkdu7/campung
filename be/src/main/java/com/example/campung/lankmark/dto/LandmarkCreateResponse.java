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
@Schema(description = "랜드마크 등록 응답")
public class LandmarkCreateResponse {
    
    @Schema(description = "생성된 랜드마크 ID", example = "1")
    private Long id;
    
    @Schema(description = "랜드마크 이름", example = "중앙도서관")
    private String name;
    
    @Schema(description = "생성 시간", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
}