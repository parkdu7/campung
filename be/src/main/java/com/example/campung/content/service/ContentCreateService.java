package com.example.campung.content.service;

import com.example.campung.content.dto.ContentCreateRequest;
import com.example.campung.content.dto.ContentCreateResponse;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.lankmark.service.LandmarkSearchService;
import com.example.campung.lankmark.entity.Landmark;
import com.example.campung.entity.Content;
import com.example.campung.entity.Attachment;
import com.example.campung.entity.User;
import com.example.campung.notification.service.PostEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ContentCreateService {
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private ThumbnailService thumbnailService;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostEventPublisher postEventPublisher;
    
    @Autowired
    private LandmarkSearchService landmarkSearchService;
    
    @Autowired
    private FileSizeValidationService fileSizeValidationService;
    
    @Transactional
    public ContentCreateResponse createContent(ContentCreateRequest request, String accessToken) throws IOException {
        log.info("=== CONTENT 생성 시작 ===");
        log.info("accessToken: {}", accessToken);
        log.info("title: {}", request.getTitle());
        
        validateContentRequest(request);
        log.info("유효성 검증 완료");
        
        User author = userRepository.findByUserId(accessToken)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(accessToken)
                            .nickname(accessToken)
                            .passwordHash("temp_hash")
                            .build();
                    return userRepository.save(newUser);
                });
        log.info("User 조회/생성 완료: {}", author.getUserId());
        
        // 주변 랜드마크를 찾아서 건물 이름 설정
        String buildingName = findNearbyLandmarkName(request.getLatitude(), request.getLongitude());

        Content.ContentBuilder contentBuilder = Content.builder()
                .title(request.getTitle())
                .content(request.getBody())
                .author(author)
                .postType(request.getPostType())
                .emotion(request.getEmotionTag())
                .buildingName(buildingName);
        
        if (request.getLatitude() != null && request.getLongitude() != null) {
            contentBuilder.latitude(BigDecimal.valueOf(request.getLatitude()))
                         .longitude(BigDecimal.valueOf(request.getLongitude()));
        }
        log.info("ContentBuilder 설정 완료");
        
        Content content = contentBuilder.build();
        log.info("Content 생성 완료");
        
        List<Attachment> attachments = new ArrayList<>();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            log.info("파일 처리 시작: {}개", request.getFiles().size());
            int index = 1;
            for (MultipartFile file : request.getFiles()) {
                if (!file.isEmpty()) {
                    fileSizeValidationService.validateFileSize(file);

                    String fileUrl = s3Service.uploadFile(file);
                    String thumbnailUrl = null;
                    
                    // 이미지 파일인 경우 썸네일 생성
                    if (thumbnailService.canGenerateThumbnail(file)) {
                        thumbnailUrl = generateThumbnailSafely(file);
                    }
                    
                    Attachment attachment = Attachment.builder()
                            .originalName(file.getOriginalFilename())
                            .url(fileUrl)
                            .thumbnailUrl(thumbnailUrl)
                            .fileSize((int) file.getSize())
                            .fileType(file.getContentType())
                            .idx(index++)
                            .content(content)
                            .build();
                    
                    attachments.add(attachment);
                }
            }
        }
        
        content.setAttachments(attachments);
        
        Content savedContent = contentRepository.save(content);
        log.info("=== CONTENT DB 저장 완료 ===");
        log.info("저장된 Content ID: {}", savedContent.getContentId());
        
        // 새 게시글 알림 이벤트 발행
        if (savedContent.getLatitude() != null && savedContent.getLongitude() != null) {
            double lat = savedContent.getLatitude().doubleValue();
            double lon = savedContent.getLongitude().doubleValue();
            postEventPublisher.publishNewPost(savedContent.getContentId(), lat, lon);
            log.info("=== 새 게시글 이벤트 발행 완료 ===");
            log.info("좌표: lat={}, lon={}", lat, lon);
        }
        
        return new ContentCreateResponse(true, "게시글이 성공적으로 작성되었습니다", savedContent.getContentId());
    }
    
    private String findNearbyLandmarkName(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        
        List<Landmark> nearbyLandmarks = landmarkSearchService.findNearbyLandmarks(
            latitude, longitude, 100); // 100m 반경
        
        if (!nearbyLandmarks.isEmpty()) {
            String buildingName = nearbyLandmarks.get(0).getName();
            log.info("주변 랜드마크 발견: {}", buildingName);
            return buildingName;
        }
        
        return null;
    }
    
    private String generateThumbnailSafely(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("썸네일 생성 시작: {}", fileName);
        
        String thumbnailUrl = null;
        if (thumbnailService.canGenerateThumbnail(file)) {
            log.warn("썸네일 생성은 현재 비활성화되어 있습니다: {}", fileName);
        }
        
        return thumbnailUrl;
    }
    
    private void validateContentRequest(ContentCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목을 입력해주세요");
        }
        
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("내용을 입력해주세요");
        }
        
        if (request.getPostType() == null) {
            throw new IllegalArgumentException("게시글 타입을 선택해주세요");
        }
        
        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("익명 여부를 설정해주세요");
        }
    }
}