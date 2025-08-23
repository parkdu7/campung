package com.example.Campung.Content.service;

import com.example.Campung.Content.dto.ContentDeleteRequest;
import com.example.Campung.Content.dto.ContentDeleteResponse;
import com.example.Campung.Content.repository.ContentRepository;
import com.example.Campung.Global.Exception.ContentNotFoundException;
import com.example.Campung.Global.Exception.UnauthorizedException;
import com.example.Campung.entity.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentDeleteService {
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Transactional
    public ContentDeleteResponse deleteContent(Long contentId, String accessToken, ContentDeleteRequest request) {
        System.out.println("=== CONTENT 삭제 시작 ===");
        System.out.println("contentId: " + contentId);
        System.out.println("accessToken: " + accessToken);
        
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        
        // 작성자 권한 확인
        if (!content.getAuthor().getUserId().equals(accessToken)) {
            throw new UnauthorizedException("게시글 삭제 권한이 없습니다");
        }
        
        System.out.println("권한 확인 완료");
        
        // 삭제 사유 로깅 (필요시)
        if (request != null && request.getReason() != null) {
            System.out.println("삭제 사유: " + request.getReason());
        }
        
        // 게시글 삭제 (Cascade로 연관된 Attachment도 자동 삭제됨)
        contentRepository.delete(content);
        
        System.out.println("=== CONTENT 삭제 완료 ===");
        System.out.println("삭제된 Content ID: " + contentId);
        
        return new ContentDeleteResponse(true, "게시글이 성공적으로 삭제되었습니다", contentId);
    }
    
    @Transactional
    public ContentDeleteResponse deleteContent(Long contentId, String accessToken) {
        return deleteContent(contentId, accessToken, null);
    }
}