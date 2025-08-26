package com.example.campung.lankmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandmarkSummaryResponse {
    private Long landmarkId;
    private String summary;
    private LocalDateTime generatedAt;
    private Integer postCount;
    private List<String> keywords;
}