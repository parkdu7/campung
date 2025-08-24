package com.example.campung.content.service;

import com.example.campung.content.dto.ContentCreateRequest;
import com.example.campung.content.dto.ContentCreateResponse;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.entity.Content;
import com.example.campung.entity.Attachment;
import com.example.campung.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    
    @Transactional
    public ContentCreateResponse createContent(ContentCreateRequest request, String accessToken) throws IOException {
        System.out.println("=== CONTENT 생성 시작 ===");
        System.out.println("accessToken: " + accessToken);
        System.out.println("title: " + request.getTitle());
        
        validateContentRequest(request);
        System.out.println("유효성 검증 완료");
        
        User author = userRepository.findByUserId(accessToken)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(accessToken)
                            .nickname(accessToken)
                            .passwordHash("temp_hash")
                            .build();
                    return userRepository.save(newUser);
                });
        System.out.println("User 조회/생성 완료: " + author.getUserId());
        
        Content.ContentBuilder contentBuilder = Content.builder()
                .title(request.getTitle())
                .content(request.getBody())
                .author(author)
                .postType(request.getPostType())
                .emotion(request.getEmotionTag());
        
        if (request.getLatitude() != null && request.getLongitude() != null) {
            contentBuilder.latitude(BigDecimal.valueOf(request.getLatitude()))
                         .longitude(BigDecimal.valueOf(request.getLongitude()));
        }
        System.out.println("ContentBuilder 설정 완료");
        
        Content content = contentBuilder.build();
        System.out.println("Content 생성 완료");
        
        List<Attachment> attachments = new ArrayList<>();
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            System.out.println("파일 처리 시작: " + request.getFiles().size());
            int index = 1;
            for (MultipartFile file : request.getFiles()) {
                if (!file.isEmpty()) {
                    String fileUrl = s3Service.uploadFile(file);
                    String thumbnailUrl = null;
                    
                    // 이미지 파일인 경우 썸네일 생성
                    if (thumbnailService.canGenerateThumbnail(file)) {
                        try {
                            System.out.println("썸네일 생성 시작: " + file.getOriginalFilename());
                            java.io.InputStream thumbnailStream = thumbnailService.generateImageThumbnailAsStream(file);
                            byte[] thumbnailBytes = thumbnailService.generateImageThumbnail(file);
                            thumbnailUrl = s3Service.uploadThumbnail(
                                new java.io.ByteArrayInputStream(thumbnailBytes), 
                                thumbnailBytes.length, 
                                file.getOriginalFilename()
                            );
                            System.out.println("썸네일 생성 완료: " + thumbnailUrl);
                        } catch (Exception e) {
                            System.out.println("썸네일 생성 실패: " + e.getMessage());
                            // 썸네일 생성 실패해도 원본 파일 업로드는 계속 진행
                        }
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
        System.out.println("=== CONTENT DB 저장 완료 ===");
        System.out.println("저장된 Content ID: " + savedContent.getContentId());
        
        return new ContentCreateResponse(true, "게시글이 성공적으로 작성되었습니다", savedContent.getContentId());
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
        
        if (request.getEmotionTag() == null || request.getEmotionTag().trim().isEmpty()) {
            throw new IllegalArgumentException("감정 태그를 선택해주세요");
        }
        
        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("익명 여부를 설정해주세요");
        }
    }
}