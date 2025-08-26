package com.example.campung.lankmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandmarkDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private String imageUrl;
    private String category;
    private Double latitude;
    private Double longitude;
    private String currentSummary;
    private String summaryUpdatedAt;
    private String createdAt;
}