package com.example.campung.lankmark.service;

import com.example.campung.lankmark.dto.*;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.lankmark.repository.LandmarkRepository;
import com.example.campung.global.enums.LandmarkCategory;
import com.example.campung.global.exception.LandmarkNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LandmarkCrudService {

    private final LandmarkRepository landmarkRepository;
    private final LandmarkValidationService validationService;
    private final LandmarkImageService imageService;

    /**
     * ID로 랜드마크 조회
     */
    public Landmark findById(Long landmarkId) {
        return landmarkRepository.findById(landmarkId)
                .orElseThrow(() -> new LandmarkNotFoundException(landmarkId));
    }
    
    /**
     * 랜드마크 상세 조회
     */
    public LandmarkDetailResponse getLandmarkDetail(Long landmarkId) {
        Landmark landmark = findById(landmarkId);
        
        return LandmarkDetailResponse.builder()
                .id(landmark.getId())
                .name(landmark.getName())
                .description(landmark.getDescription())
                .thumbnailUrl(landmark.getThumbnailUrl())
                .imageUrl(landmark.getImageUrl())
                .category(landmark.getCategory().getDescription())
                .latitude(landmark.getLatitude())
                .longitude(landmark.getLongitude())
                .currentSummary(landmark.getCurrentSummary())
                .summaryUpdatedAt(landmark.getSummaryUpdatedAt() != null ? 
                    landmark.getSummaryUpdatedAt().toString() : null)
                .createdAt(landmark.getCreatedAt().toString())
                .build();
    }

    /**
     * 랜드마크 생성 (Form Request 방식)
     */
    @Transactional
    public LandmarkCreateResponse createLandmarkWithFormRequest(LandmarkCreateFormRequest formRequest) {
        return createLandmarkWithImage(
                formRequest.getName(),
                formRequest.getDescription(),
                formRequest.getLatitude(),
                formRequest.getLongitude(),
                formRequest.getCategory().name(),
                formRequest.getRadius(),
                formRequest.getImageFile()
        );
    }

    /**
     * 랜드마크 생성 (이미지 포함)
     */
    @Transactional
    public LandmarkCreateResponse createLandmarkWithImage(String name, String description, 
            Double latitude, Double longitude, String categoryStr, Integer radius,
            org.springframework.web.multipart.MultipartFile imageFile) {
        
        // 1. 입력 파라미터 유효성 검증
        validationService.validateLandmarkInputs(name, latitude, longitude, categoryStr);
        
        // 2. 카테고리 변환
        LandmarkCategory category = LandmarkCategory.valueOf(categoryStr.toUpperCase());
        
        // 3. radius 설정 (없으면 카테고리 기본값 사용)
        int finalRadius = radius != null ? radius : category.getDefaultRadius();
        
        // 4. 중복 랜드마크 체크
        validationService.checkDuplicateLandmark(latitude, longitude, name);
        
        // 5. 이미지 처리 (업로드 및 썸네일 생성)
        LandmarkImageService.ImageUploadResult imageResult = imageService.uploadLandmarkImage(imageFile);
        
        // 6. 랜드마크 생성
        Landmark landmark = Landmark.builder()
                .name(name)
                .description(description)
                .latitude(latitude)
                .longitude(longitude)
                .category(category)
                .radius(finalRadius)
                .imageUrl(imageResult.getImageUrl())
                .thumbnailUrl(imageResult.getThumbnailUrl())
                .build();
        
        // 6. 저장
        Landmark savedLandmark = landmarkRepository.save(landmark);
        
        log.info("새 랜드마크 등록 완료: {} (ID: {}), 이미지: {}, 썸네일: {}", 
                savedLandmark.getName(), savedLandmark.getId(), 
                imageResult.hasImage(), imageResult.hasThumbnail());
        
        return LandmarkCreateResponse.builder()
                .id(savedLandmark.getId())
                .name(savedLandmark.getName())
                .createdAt(savedLandmark.getCreatedAt())
                .build();
    }

    /**
     * 랜드마크 수정
     */
    @Transactional
    public LandmarkUpdateResponse updateLandmark(Long landmarkId, LandmarkUpdateRequest updateRequest) {
        // 1. 랜드마크 존재 확인
        Landmark landmark = findById(landmarkId);
        
        // 2. 입력 파라미터 유효성 검증
        validationService.validateLandmarkInputs(updateRequest.getName(), updateRequest.getLatitude(), 
                              updateRequest.getLongitude(), updateRequest.getCategory().name());
        
        // 3. 중복 랜드마크 체크 (현재 랜드마크 제외)
        validationService.checkDuplicateLandmarkForUpdate(landmarkId, updateRequest.getLatitude(), 
                                       updateRequest.getLongitude(), updateRequest.getName());
        
        // 4. radius 설정 (없으면 카테고리 기본값 사용)
        int finalRadius = updateRequest.getRadius() != null ? updateRequest.getRadius() : updateRequest.getCategory().getDefaultRadius();
        
        // 5. 이미지 처리 (새 이미지가 있는 경우)
        String newImageUrl = landmark.getImageUrl();
        String newThumbnailUrl = landmark.getThumbnailUrl();
        
        if (updateRequest.getImageFile() != null && !updateRequest.getImageFile().isEmpty()) {
            log.info("랜드마크 이미지 수정 시작: {}", updateRequest.getImageFile().getOriginalFilename());
            
            LandmarkImageService.ImageUploadResult imageResult = 
                imageService.uploadLandmarkImage(updateRequest.getImageFile());
            
            newImageUrl = imageResult.getImageUrl();
            newThumbnailUrl = imageResult.getThumbnailUrl();
        }
        
        // 6. 랜드마크 정보 업데이트
        Landmark updatedLandmark = Landmark.builder()
                .id(landmark.getId())
                .name(updateRequest.getName())
                .description(updateRequest.getDescription())
                .latitude(updateRequest.getLatitude())
                .longitude(updateRequest.getLongitude())
                .category(updateRequest.getCategory())
                .radius(finalRadius)
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

    /**
     * 랜드마크 삭제
     */
    @Transactional
    public void deleteLandmark(Long landmarkId) {
        // 1. 랜드마크 존재 확인
        Landmark landmark = findById(landmarkId);
        
        // 2. 랜드마크 삭제
        landmarkRepository.delete(landmark);
        
        log.info("랜드마크 삭제 완료: {} (ID: {})", landmark.getName(), landmarkId);
    }
}