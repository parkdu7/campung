package com.example.campung.content.service;

import com.example.campung.content.dto.ContentUpdateRequest;
import com.example.campung.content.dto.ContentUpdateResponse;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.global.exception.ContentNotFoundException;
import com.example.campung.global.exception.UnauthorizedException;
import com.example.campung.entity.Content;
import com.example.campung.entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentUpdateService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private S3Service s3Service;
    
    @Transactional
    public ContentUpdateResponse updateContent(Long contentId, ContentUpdateRequest request, String accessToken) throws IOException {
        System.out.println("=== CONTENT 수정 시작 ===");
        System.out.println("contentId: " + contentId);
        System.out.println("accessToken: " + accessToken);
        
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        
        // 작성자 권한 확인
        if (!content.getAuthor().getUserId().equals(accessToken)) {
            throw new UnauthorizedException("게시글 수정 권한이 없습니다");
        }
        
        validateUpdateRequest(request);
        System.out.println("유효성 검증 완료");
            
            // 기본 정보 업데이트
            if (request.getTitle() != null) {
                content.setTitle(request.getTitle());
            }
            if (request.getBody() != null) {
                content.setContent(request.getBody());
            }
            if (request.getPostType() != null) {
                content.setPostType(request.getPostType());
            }
            if (request.getEmotionTag() != null) {
                content.setEmotion(request.getEmotionTag());
            }
            
            // 위치 정보 업데이트
            if (request.getLatitude() != null && request.getLongitude() != null) {
                content.setLatitude(BigDecimal.valueOf(request.getLatitude()));
                content.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            }
            
            // 삭제할 파일들 처리
            if (request.getDeleteFileIds() != null && !request.getDeleteFileIds().isEmpty()) {
                List<Attachment> currentAttachments = content.getAttachments();
                List<Attachment> remainingAttachments = currentAttachments.stream()
                        .filter(attachment -> !request.getDeleteFileIds().contains(attachment.getAttachmentId()))
                        .collect(Collectors.toList());
                content.setAttachments(remainingAttachments);
                System.out.println("삭제할 파일 수: " + request.getDeleteFileIds().size());
            }
            
            // 새로운 파일들 추가
            if (request.getNewFiles() != null && !request.getNewFiles().isEmpty()) {
                System.out.println("새 파일 처리 시작: " + request.getNewFiles().size());
                List<Attachment> currentAttachments = content.getAttachments();
                if (currentAttachments == null) {
                    currentAttachments = new ArrayList<>();
                }
                
                int nextIndex = currentAttachments.size() + 1;
                for (MultipartFile file : request.getNewFiles()) {
                    if (!file.isEmpty()) {
                        String fileUrl = s3Service.uploadFile(file);
                        
                        Attachment attachment = Attachment.builder()
                                .originalName(file.getOriginalFilename())
                                .url(fileUrl)
                                .fileSize((int) file.getSize())
                                .fileType(file.getContentType())
                                .idx(nextIndex++)
                                .content(content)
                                .build();
                        
                        currentAttachments.add(attachment);
                    }
                }
                content.setAttachments(currentAttachments);
            }
            
        Content updatedContent = contentRepository.save(content);
        System.out.println("=== CONTENT 수정 완료 ===");
        System.out.println("수정된 Content ID: " + updatedContent.getContentId());
        
        return new ContentUpdateResponse(true, "게시글이 성공적으로 수정되었습니다", updatedContent.getContentId());
    }
    
    private void validateUpdateRequest(ContentUpdateRequest request) {
        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목을 입력해주세요");
        }
        
        if (request.getBody() != null && request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("내용을 입력해주세요");
        }
        
        if (request.getPostType() != null && request.getPostType().toString().trim().isEmpty()) {
            throw new IllegalArgumentException("게시글 타입을 선택해주세요");
        }
        
        if (request.getEmotionTag() != null && request.getEmotionTag().trim().isEmpty()) {
            throw new IllegalArgumentException("감정 태그를 선택해주세요");
        }
    }
}