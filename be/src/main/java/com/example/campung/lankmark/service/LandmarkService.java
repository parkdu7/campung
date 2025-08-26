package com.example.campung.lankmark.service;

import com.example.campung.lankmark.dto.LandmarkCreateResponse;
import com.example.campung.lankmark.dto.LandmarkCreateFormRequest;
import com.example.campung.lankmark.dto.LandmarkUpdateRequest;
import com.example.campung.lankmark.dto.LandmarkUpdateResponse;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import com.example.campung.global.enums.LandmarkCategory;
import com.example.campung.content.service.S3Service;
import com.example.campung.content.service.ThumbnailService;
import com.example.campung.global.exception.LandmarkNotFoundException;
import com.example.campung.global.exception.DuplicateLandmarkException;
import com.example.campung.global.exception.InvalidCoordinateException;
import com.example.campung.global.exception.ImageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LandmarkService {

    private final LandmarkRepository landmarkRepository;
    private final S3Service s3Service;
    private final ThumbnailService thumbnailService;
    
    private static final double DUPLICATE_DISTANCE_THRESHOLD = 100.0; // 100미터


    private void validateCoordinates(Double latitude, Double longitude) {
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

    private void checkDuplicateLandmark(Double latitude, Double longitude, String name) {
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
    
    public Landmark findById(Long landmarkId) {
        return landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> new LandmarkNotFoundException(landmarkId));
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
    
    @Transactional
    public LandmarkCreateResponse createLandmarkWithImage(String name, String description, 
            Double latitude, Double longitude, String categoryStr, MultipartFile imageFile) {
        
        // 1. 입력 파라미터 유효성 검증
        validateLandmarkInputs(name, latitude, longitude, categoryStr);
        
        // 2. 카테고리 변환
        LandmarkCategory category = LandmarkCategory.valueOf(categoryStr.toUpperCase());
        
        // 3. 중복 랜드마크 체크
        checkDuplicateLandmark(latitude, longitude, name);
        
        // 4. 이미지 처리 (업로드 및 썸네일 생성)
        String imageUrl = null;
        String thumbnailUrl = null;
        
        if (imageFile != null && !imageFile.isEmpty()) {
            log.info("랜드마크 이미지 처리 시작: {}", imageFile.getOriginalFilename());
            
            try {
                // 원본 이미지 업로드
                imageUrl = s3Service.uploadFile(imageFile);
                log.info("원본 이미지 업로드 완료: {}", imageUrl);
                
                // 썸네일 생성 및 업로드
                if (thumbnailService.canGenerateThumbnail(imageFile)) {
                    try {
                        java.io.InputStream thumbnailStream = thumbnailService.generateImageThumbnailAsStream(imageFile);
                        byte[] thumbnailBytes = thumbnailService.generateImageThumbnail(imageFile);
                        thumbnailUrl = s3Service.uploadThumbnail(
                            new java.io.ByteArrayInputStream(thumbnailBytes), 
                            thumbnailBytes.length, 
                            imageFile.getOriginalFilename()
                        );
                        log.info("썸네일 생성 및 업로드 완료: {}", thumbnailUrl);
                    } catch (Exception e) {
                        log.warn("썸네일 생성 실패, 원본 이미지만 사용: {}", e.getMessage());
                    }
                }
            } catch (java.io.IOException e) {
                throw new ImageProcessingException("이미지 처리 중 오류가 발생했습니다.", e);
            }
        }
        
        // 5. 랜드마크 생성
        Landmark landmark = Landmark.builder()
                .name(name)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .category(category)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .build();
        
        // 6. 저장
        Landmark savedLandmark = landmarkRepository.save(landmark);
        
        log.info("새 랜드마크 등록 완료: {} (ID: {}), 이미지: {}, 썸네일: {}", 
                savedLandmark.getName(), savedLandmark.getId(), imageUrl != null, thumbnailUrl != null);
        
        return LandmarkCreateResponse.builder()
                .id(savedLandmark.getId())
                .name(savedLandmark.getName())
                .createdAt(savedLandmark.getCreatedAt())
                .build();
    }
    
    private void validateLandmarkInputs(String name, Double latitude, Double longitude, String category) {
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
    
    @Transactional
    public LandmarkCreateResponse createLandmarkWithFormRequest(LandmarkCreateFormRequest formRequest) {
        return createLandmarkWithImage(
                formRequest.getName(),
                formRequest.getDescription(),
                formRequest.getLatitude(),
                formRequest.getLongitude(),
                formRequest.getCategory().name(),
                formRequest.getImageFile()
        );
    }
    
    @Transactional
    public LandmarkUpdateResponse updateLandmark(Long landmarkId, LandmarkUpdateRequest updateRequest) {
        // 1. 랜드마크 존재 확인
        Landmark landmark = findById(landmarkId);
        
        // 2. 입력 파라미터 유효성 검증
        validateLandmarkInputs(updateRequest.getName(), updateRequest.getLatitude(), 
                              updateRequest.getLongitude(), updateRequest.getCategory().name());
        
        // 3. 중복 랜드마크 체크 (현재 랜드마크 제외)
        checkDuplicateLandmarkForUpdate(landmarkId, updateRequest.getLatitude(), 
                                       updateRequest.getLongitude(), updateRequest.getName());
        
        // 4. 이미지 처리 (새 이미지가 있는 경우)
        String newImageUrl = landmark.getImageUrl();
        String newThumbnailUrl = landmark.getThumbnailUrl();
        
        if (updateRequest.getImageFile() != null && !updateRequest.getImageFile().isEmpty()) {
            log.info("랜드마크 이미지 수정 시작: {}", updateRequest.getImageFile().getOriginalFilename());
            
            try {
                // 새 이미지 업로드
                newImageUrl = s3Service.uploadFile(updateRequest.getImageFile());
                log.info("새 이미지 업로드 완료: {}", newImageUrl);
                
                // 새 썸네일 생성 및 업로드
                if (thumbnailService.canGenerateThumbnail(updateRequest.getImageFile())) {
                    try {
                        byte[] thumbnailBytes = thumbnailService.generateImageThumbnail(updateRequest.getImageFile());
                        newThumbnailUrl = s3Service.uploadThumbnail(
                            new java.io.ByteArrayInputStream(thumbnailBytes), 
                            thumbnailBytes.length, 
                            updateRequest.getImageFile().getOriginalFilename()
                        );
                        log.info("새 썸네일 생성 및 업로드 완료: {}", newThumbnailUrl);
                    } catch (Exception e) {
                        log.warn("썸네일 생성 실패, 원본 이미지만 사용: {}", e.getMessage());
                    }
                }
            } catch (java.io.IOException e) {
                throw new ImageProcessingException("이미지 처리 중 오류가 발생했습니다.", e);
            }
        }
        
        // 5. 랜드마크 정보 업데이트
        Landmark updatedLandmark = Landmark.builder()
                .id(landmark.getId())
                .name(updateRequest.getName())
                .description(updateRequest.getDescription())
                .latitude(updateRequest.getLatitude())
                .longitude(updateRequest.getLongitude())
                .category(updateRequest.getCategory())
                .imageUrl(newImageUrl)
                .thumbnailUrl(newThumbnailUrl)
                .currentSummary(landmark.getCurrentSummary())
                .summaryUpdatedAt(landmark.getSummaryUpdatedAt())
                .createdAt(landmark.getCreatedAt())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        
        Landmark savedLandmark = landmarkRepository.save(updatedLandmark);
        
        log.info("랜드마크 수정 완료: {} (ID: {})", savedLandmark.getName(), savedLandmark.getId());
        
        return LandmarkUpdateResponse.builder()
                .id(savedLandmark.getId())
                .updatedAt(savedLandmark.getUpdatedAt())
                .build();
    }
    
    @Transactional
    public void deleteLandmark(Long landmarkId) {
        // 1. 랜드마크 존재 확인
        Landmark landmark = findById(landmarkId);
        
        // 2. 랜드마크 삭제
        landmarkRepository.delete(landmark);
        
        log.info("랜드마크 삭제 완료: {} (ID: {})", landmark.getName(), landmarkId);
    }
    
    private void checkDuplicateLandmarkForUpdate(Long currentLandmarkId, Double latitude, Double longitude, String name) {
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