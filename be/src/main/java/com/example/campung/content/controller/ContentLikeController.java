package com.example.campung.content.controller;

import com.example.campung.content.dto.ContentLikeResponse;
import com.example.campung.content.service.ContentLikeService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/contents")
@Tag(name = "Content", description = "콘텐츠 관련 API")
public class ContentLikeController {
    
    @Autowired
    private ContentLikeService contentLikeService;
    
    @Operation(summary = "콘텐츠 좋아요 토글", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{contentId}/like")
    public ResponseEntity<ContentLikeResponse> toggleLike(
            @PathVariable Long contentId,
            @Parameter(description = "인증 토큰", example = "Bearer test", required = true)
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ContentLikeResponse errorResponse = new ContentLikeResponse(false, "인증 토큰이 필요합니다", null);
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String accessToken = authorization.substring(7);
        ContentLikeResponse response = contentLikeService.toggleLike(contentId, accessToken);
        
        return ResponseEntity.ok(response);
    }
}