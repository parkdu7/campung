package com.example.campung.lankmark.service;

import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import com.example.campung.lankmark.dto.MapPOIRequest;
import com.example.campung.lankmark.dto.MapPOIResponse;
import com.example.campung.global.enums.LandmarkCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandmarkSearchService {

    private final LandmarkRepository landmarkRepository;
    private final LandmarkValidationService validationService;

    /**
     * 위치 기반 주변 랜드마크 조회
     */
    public List<Landmark> findNearbyLandmarks(Double latitude, Double longitude, Integer radius) {
        validationService.validateCoordinates(latitude, longitude);
        
        int searchRadius = radius != null ? radius : 1000; // 기본 1km
        
        return landmarkRepository.findNearbyLandmarks(latitude, longitude, searchRadius);
    }
    
    /**
     * 이름으로 랜드마크 검색
     */
    public List<Landmark> findByNameContaining(String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        
        return landmarkRepository.findByNameContainingIgnoreCase(name.trim());
    }
    
    /**
     * 요약이 있는 랜드마크 조회
     */
    public List<Landmark> findLandmarksWithSummary() {
        return landmarkRepository.findLandmarksWithSummary();
    }
    
    /**
     * 맵 POI 목록 조회
     */
    public MapPOIResponse getMapPOIs(MapPOIRequest request) {
        validationService.validateCoordinates(request.getLatitude(), request.getLongitude());
        
        int searchRadius = request.getRadius() != null ? request.getRadius() : 1000; // 기본 1km
        
        List<Landmark> landmarks = landmarkRepository.findNearbyLandmarks(
            request.getLatitude(), request.getLongitude(), searchRadius);
        
        // 카테고리 필터링 (선택사항)
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            try {
                LandmarkCategory categoryEnum = LandmarkCategory.valueOf(request.getCategory().toUpperCase());
                landmarks = landmarks.stream()
                    .filter(landmark -> landmark.getCategory() == categoryEnum)
                    .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // 잘못된 카테고리인 경우 모든 랜드마크 반환
                log.warn("잘못된 카테고리 필터: {}", request.getCategory());
            }
        }
        
        List<MapPOIResponse.MapPOIItem> poiItems = landmarks.stream()
            .map(landmark -> MapPOIResponse.MapPOIItem.builder()
                .id(landmark.getId())
                .name(landmark.getName())
                .latitude(landmark.getLatitude())
                .longitude(landmark.getLongitude())
                .thumbnailUrl(landmark.getThumbnailUrl())
                .category(landmark.getCategory().getDescription())
                .build())
            .collect(java.util.stream.Collectors.toList());
        
        return MapPOIResponse.builder()
            .success(true)
            .message("맵 POI 조회 성공")
            .data(poiItems)
            .build();
    }
}