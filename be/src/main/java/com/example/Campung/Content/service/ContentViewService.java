package com.example.Campung.Content.Service;

import com.example.Campung.Content.Dto.ContentDetailResponse;
import com.example.Campung.Content.Dto.ContentDetailRequest;
import com.example.Campung.Content.Repository.ContentRepository;
import com.example.Campung.Global.Exception.ContentNotFoundException;
import com.example.Campung.Entity.Content;
import com.example.Campung.Entity.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentViewService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private ContentHotService contentHotService;
    
    public ContentDetailResponse getContentById(Long contentId) {
        System.out.println("=== CONTENT 조회 시작 ===");
        System.out.println("contentId: " + contentId);
        
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        
        System.out.println("Content 조회 완료: " + content.getTitle());
        
        ContentDetailRequest contentDetail = buildContentDetail(content);
        
        return new ContentDetailResponse(true, "게시글 조회 성공", contentDetail);
    }
    
    private ContentDetailRequest buildContentDetail(Content content) {
        ContentDetailRequest detail = new ContentDetailRequest();
        
        detail.setContentId(content.getContentId());
        detail.setUserId(content.getAuthor().getUserId());
        detail.setPostType(content.getPostType().name());
        detail.setTitle(content.getTitle());
        detail.setBody(content.getContent());
        
        // Author 정보 설정
        ContentDetailRequest.AuthorInfo author = new ContentDetailRequest.AuthorInfo(
                content.getAuthor().getNickname(),
                null, // profileImageUrl - 추후 구현
                false // isAnonymous - 추후 구현
        );
        detail.setAuthor(author);
        
        // Location 정보 설정
        if (content.getLatitude() != null && content.getLongitude() != null) {
            ContentDetailRequest.LocationInfo location = new ContentDetailRequest.LocationInfo(
                    content.getLatitude().doubleValue(),
                    content.getLongitude().doubleValue(),
                    null, // address - 추후 구현
                    null  // buildingName - 추후 구현
            );
            detail.setLocation(location);
        }
        
        // MediaFiles 정보 설정
        if (content.getAttachments() != null) {
            List<ContentDetailRequest.MediaFileInfo> mediaFiles = content.getAttachments().stream()
                    .map(this::convertToMediaFileInfo)
                    .collect(Collectors.toList());
            detail.setMediaFiles(mediaFiles);
        }
        
        // HOT 컨텐츠 여부 설정
        detail.setHotContent(contentHotService.isHotContent(content.getContentId()));
        
        return detail;
    }
    
    private ContentDetailRequest.MediaFileInfo convertToMediaFileInfo(Attachment attachment) {
        String fileType = determineFileTypeFromUrl(attachment.getUrl());
        
        return new ContentDetailRequest.MediaFileInfo(
                attachment.getAttachmentId(),
                fileType,
                attachment.getUrl(),
                null, // thumbnailUrl - 추후 구현
                attachment.getOriginalName(),
                attachment.getFileSize(),
                attachment.getIdx()
        );
    }
    
    private String determineFileTypeFromUrl(String fileUrl) {
        if (fileUrl.contains("/images/")) {
            return "IMAGE";
        } else if (fileUrl.contains("/videos/")) {
            return "VIDEO";
        } else {
            return "AUDIO";
        }
    }
}