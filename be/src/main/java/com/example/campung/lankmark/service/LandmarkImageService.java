package com.example.campung.lankmark.service;

import com.example.campung.content.service.S3Service;
import com.example.campung.content.service.ThumbnailService;
import com.example.campung.global.exception.ImageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandmarkImageService {

    private final S3Service s3Service;
    private final ThumbnailService thumbnailService;

    /**
     * 이미지 업로드 및 썸네일 생성
     */
    public ImageUploadResult uploadLandmarkImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return new ImageUploadResult(null, null);
        }

        log.info("랜드마크 이미지 처리 시작: {}", imageFile.getOriginalFilename());
        
        try {
            // 원본 이미지 업로드
            String imageUrl = s3Service.uploadFile(imageFile);
            log.info("원본 이미지 업로드 완료: {}", imageUrl);
            
            String thumbnailUrl = null;
            
            // 썸네일 생성 및 업로드
            if (thumbnailService.canGenerateThumbnail(imageFile)) {
                try {
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
            
            return new ImageUploadResult(imageUrl, thumbnailUrl);
            
        } catch (IOException e) {
            throw new ImageProcessingException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 업로드 결과를 담는 클래스
     */
    public static class ImageUploadResult {
        private final String imageUrl;
        private final String thumbnailUrl;

        public ImageUploadResult(String imageUrl, String thumbnailUrl) {
            this.imageUrl = imageUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getImageUrl() { return imageUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        
        public boolean hasImage() { return imageUrl != null; }
        public boolean hasThumbnail() { return thumbnailUrl != null; }
    }
}