package com.example.campung.lankmark.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapPOIRequest {
    @Schema(description = "위도", example = "36.0", required = true)
    private Double latitude;
    
    @Schema(description = "경도", example = "127.0", required = true)
    private Double longitude;
    private Integer radius; // 반경(미터)
    private String category; // 카테고리 필터 (선택사항)
}