package com.example.campung.lankmark.dto;

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
    private Double latitude;
    private Double longitude;
    private Integer radius; // 반경(미터)
    private String category; // 카테고리 필터 (선택사항)
}