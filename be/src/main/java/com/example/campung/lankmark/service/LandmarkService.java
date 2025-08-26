package com.example.campung.lankmark.service;

import com.example.campung.lankmark.dto.LandmarkCreateRequest;
import com.example.campung.lankmark.dto.LandmarkCreateResponse;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LandmarkService {

    private final LandmarkRepository landmarkRepository;
    
    private static final double DUPLICATE_DISTANCE_THRESHOLD = 100.0; // 100미터

    @Transactional
    public LandmarkCreateResponse createLandmark(LandmarkCreateRequest request) {
        // 1. 좌표 유효성 검증
        validateCoordinates(request.getLatitude(), request.getLongitude());
        
        // 2. 중복 랜드마크 체크
        checkDuplicateLandmark(request.getLatitude(), request.getLongitude(), request.getName());
        
        // 3. 랜드마크 생성
        Landmark landmark = Landmark.builder()
                .name(request.getName())
                .description(request.getDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .build();
        
        // 4. 저장
        Landmark savedLandmark = landmarkRepository.save(landmark);
        
        log.info("새 랜드마크 등록 완료: {} (ID: {})", savedLandmark.getName(), savedLandmark.getId());
        
        return LandmarkCreateResponse.builder()
                .id(savedLandmark.getId())
                .name(savedLandmark.getName())
                .createdAt(savedLandmark.getCreatedAt())
                .build();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("위도와 경도는 필수입니다.");
        }
        
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("유효하지 않은 위도입니다. (-90 ~ 90 범위)");
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("유효하지 않은 경도입니다. (-180 ~ 180 범위)");
        }
    }

    private void checkDuplicateLandmark(Double latitude, Double longitude, String name) {
        // 같은 위치(100m 이내)에 같은 이름의 랜드마크가 있는지 확인
        List<Landmark> nearbyLandmarks = landmarkRepository.findNearbyLandmarks(
                latitude, longitude, (int) DUPLICATE_DISTANCE_THRESHOLD);
        
        boolean isDuplicate = nearbyLandmarks.stream()
                .anyMatch(landmark -> landmark.getName().equalsIgnoreCase(name.trim()));
        
        if (isDuplicate) {
            throw new IllegalArgumentException(
                    String.format("같은 위치에 '%s' 랜드마크가 이미 존재합니다.", name));
        }
        
        // 너무 가까운 위치에 다른 랜드마크가 많이 있는지 확인 (선택사항)
        if (nearbyLandmarks.size() >= 5) {
            log.warn("위치 {}:{} 주변에 랜드마크가 {}개 존재합니다.", 
                    latitude, longitude, nearbyLandmarks.size());
        }
    }
    
    public Landmark findById(Long landmarkId) {
        return landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> new IllegalArgumentException("랜드마크를 찾을 수 없습니다: " + landmarkId));
    }
    
    public List<Landmark> findNearbyLandmarks(Double latitude, Double longitude, Integer radius) {
        validateCoordinates(latitude, longitude);
        
        int searchRadius = radius != null ? radius : 1000; // 기본 1km
        
        return landmarkRepository.findNearbyLandmarks(latitude, longitude, searchRadius);
    }
    
    public List<Landmark> findByNameContaining(String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        
        return landmarkRepository.findByNameContainingIgnoreCase(name.trim());
    }
    
    public List<Landmark> findLandmarksWithSummary() {
        return landmarkRepository.findLandmarksWithSummary();
    }
}