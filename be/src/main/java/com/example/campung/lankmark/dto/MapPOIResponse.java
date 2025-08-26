package com.example.campung.lankmark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapPOIResponse {
    private boolean success;
    private String message;
    private List<MapPOIItem> data;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapPOIItem {
        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private String thumbnailUrl;
        private String category;
    }
}