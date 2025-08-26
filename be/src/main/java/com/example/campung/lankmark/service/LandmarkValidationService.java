package com.example.campung.lankmark.service;

import com.example.campung.global.enums.LandmarkCategory;
import com.example.campung.global.exception.InvalidCoordinateException;
import com.example.campung.global.exception.DuplicateLandmarkException;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandmarkValidationService {

    private final LandmarkRepository landmarkRepository;
    private static final double DUPLICATE_DISTANCE_THRESHOLD = 100.0; // 100미터

    /**
     * 좌표 유효성 검증
     */
    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidCoordinateException("위도와 경도는 필수입니다.");
        }
        
        if (latitude < -90.0 || latitude > 90.0) {
            throw new InvalidCoordinateException("유효하지 않은 위도입니다. (-90 ~ 90 범위)");
        }
        
        if (longitude < -180.0 || longitude > 180.0) {
            throw new InvalidCoordinateException("유효하지 않은 경도입니다. (-180 ~ 180 범위)");
        }
    }

    /**
     * 랜드마크 입력값 유효성 검증
     */
    public void validateLandmarkInputs(String name, Double latitude, Double longitude, String category) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("랜드마크 이름은 필수입니다.");
        }
        
        if (name.length() > 100) {
            throw new IllegalArgumentException("랜드마크 이름은 100자 이내로 입력해주세요.");
        }
        
        validateCoordinates(latitude, longitude);
        
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
        
        try {
            LandmarkCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
        }
    }

    /**
     * 중복 랜드마크 검증 (생성 시)
     */
    public void checkDuplicateLandmark(Double latitude, Double longitude, String name) {
        // 같은 위치(100m 이내)에 같은 이름의 랜드마크가 있는지 확인
        List<Landmark> nearbyLandmarks = landmarkRepository.findNearbyLandmarks(
                latitude, longitude, (int) DUPLICATE_DISTANCE_THRESHOLD);
        
        boolean isDuplicate = nearbyLandmarks.stream()
                .anyMatch(landmark -> landmark.getName().equalsIgnoreCase(name.trim()));
        
        if (isDuplicate) {
            throw DuplicateLandmarkException.forLandmark(name);
        }
        
        // 너무 가까운 위치에 다른 랜드마크가 많이 있는지 확인 (선택사항)
        if (nearbyLandmarks.size() >= 5) {
            log.warn("위치 {}:{} 주변에 랜드마크가 {}개 존재합니다.", 
                    latitude, longitude, nearbyLandmarks.size());
        }
    }

    /**
     * 중복 랜드마크 검증 (수정 시)
     */
    public void checkDuplicateLandmarkForUpdate(Long currentLandmarkId, Double latitude, Double longitude, String name) {
        // 같은 위치(100m 이내)에 같은 이름의 다른 랜드마크가 있는지 확인
        List<Landmark> nearbyLandmarks = landmarkRepository.findNearbyLandmarks(
                latitude, longitude, (int) DUPLICATE_DISTANCE_THRESHOLD);
        
        boolean isDuplicate = nearbyLandmarks.stream()
                .filter(landmark -> !landmark.getId().equals(currentLandmarkId)) // 현재 랜드마크 제외
                .anyMatch(landmark -> landmark.getName().equalsIgnoreCase(name.trim()));
        
        if (isDuplicate) {
            throw DuplicateLandmarkException.forLandmark(name);
        }
    }
}