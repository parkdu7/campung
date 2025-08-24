package com.example.campung.comment.controller;

import com.example.campung.comment.dto.CommentCreateRequest;
import com.example.campung.comment.dto.CommentCreateResponse;
import com.example.campung.comment.dto.CommentListResponse;
import com.example.campung.comment.service.CommentService;
import com.example.campung.comment.service.CommentListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.IOException;

@RestController
@RequestMapping("/api/contents")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private CommentListService commentListService;
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{contentId}/comments")
    public ResponseEntity<CommentCreateResponse> createComment(
            @PathVariable Long contentId,
            @RequestHeader("Authorization") String authorization,
            @ModelAttribute CommentCreateRequest request) throws IOException {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            CommentCreateResponse errorResponse = new CommentCreateResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String accessToken = authorization.substring(7);
        CommentCreateResponse response = commentService.createComment(contentId, request, accessToken);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{contentId}/comments/{commentId}/replies")
    public ResponseEntity<CommentCreateResponse> createReply(
            @PathVariable Long contentId,
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization,
            @ModelAttribute CommentCreateRequest request) throws IOException {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            CommentCreateResponse errorResponse = new CommentCreateResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        // 부모 댓글 ID를 자동으로 설정
        request.setParentCommentId(commentId);
        
        String accessToken = authorization.substring(7);
        CommentCreateResponse response = commentService.createComment(contentId, request, accessToken);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{contentId}/comments")
    public ResponseEntity<CommentListResponse> getComments(@PathVariable Long contentId) {
        CommentListResponse response = commentListService.getCommentsByContentId(contentId);
        return ResponseEntity.ok(response);
    }
}