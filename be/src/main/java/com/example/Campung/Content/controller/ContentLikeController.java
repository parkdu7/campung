package com.example.Campung.Content.controller;

import com.example.Campung.Content.dto.ContentLikeResponse;
import com.example.Campung.Content.service.ContentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/contents")
public class ContentLikeController {
    
    @Autowired
    private ContentLikeService contentLikeService;
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{contentId}/like")
    public ResponseEntity<ContentLikeResponse> toggleLike(
            @PathVariable Long contentId,
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