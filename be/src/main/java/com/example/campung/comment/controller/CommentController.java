package com.example.campung.comment.controller;

import com.example.campung.comment.dto.CommentCreateRequest;
import com.example.campung.comment.dto.CommentCreateResponse;
import com.example.campung.comment.dto.CommentListResponse;
import com.example.campung.comment.service.CommentService;
import com.example.campung.comment.service.CommentListService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;

@RestController
@RequestMapping("/api/contents")
@Tag(name = "Comment", description = "댓글 관련 API")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private CommentListService commentListService;
    
    @Operation(
            summary = "댓글 작성",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CommentCreateRequest.class)
                    )
            )
    )
    @PostMapping(value = "/{contentId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentCreateResponse> createComment(
            @PathVariable Long contentId,
            @Parameter(description = "인증 토큰", example = "Bearer test", required = true)
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
    
    @Operation(
            summary = "대댓글 작성",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CommentCreateRequest.class)
                    )
            )
    )
    @PostMapping(value = "/{contentId}/comments/{commentId}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentCreateResponse> createReply(
            @PathVariable Long contentId,
            @PathVariable Long commentId,
            @Parameter(description = "인증 토큰", example = "Bearer test", required = true)
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
    
    @Operation(summary = "게시글의 댓글 목록 조회")
    @GetMapping("/{contentId}/comments")
    public ResponseEntity<CommentListResponse> getComments(@PathVariable Long contentId) {
        CommentListResponse response = commentListService.getCommentsByContentId(contentId);
        return ResponseEntity.ok(response);
    }
}