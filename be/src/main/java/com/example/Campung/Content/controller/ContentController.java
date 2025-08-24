package com.example.Campung.Content.controller;

import com.example.Campung.Content.dto.ContentCreateRequest;
import com.example.Campung.Content.dto.ContentCreateResponse;
import com.example.Campung.Content.dto.ContentDetailResponse;
import com.example.Campung.Content.dto.ContentUpdateRequest;
import com.example.Campung.Content.dto.ContentUpdateResponse;
import com.example.Campung.Content.dto.ContentDeleteResponse;
import com.example.Campung.Content.dto.ContentSearchRequest;
import com.example.Campung.Content.dto.ContentSearchResponse;
import com.example.Campung.Content.dto.ContentListRequest;
import com.example.Campung.Content.dto.ContentListResponse;
import com.example.Campung.Content.service.ContentCreateService;
import com.example.Campung.Content.service.ContentViewService;
import com.example.Campung.Content.service.ContentUpdateService;
import com.example.Campung.Content.service.ContentDeleteService;
import com.example.Campung.Content.service.ContentSearchService;
import com.example.Campung.Content.service.ContentListService;
import com.example.Campung.Global.Enum.PostType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ContentController {
    
    @Autowired
    private ContentCreateService contentCreateService;
    
    @Autowired
    private ContentViewService contentViewService;
    
    @Autowired
    private ContentUpdateService contentUpdateService;
    
    @Autowired
    private ContentDeleteService contentDeleteService;
    
    @Autowired
    private ContentSearchService contentSearchService;
    
    @Autowired
    private ContentListService contentListService;
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/contents")
    public ResponseEntity<ContentCreateResponse> createContent(
            @RequestHeader("Authorization") String authorization,
            @ModelAttribute ContentCreateRequest request) throws IOException {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ContentCreateResponse errorResponse = new ContentCreateResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String accessToken = authorization.substring(7);
        ContentCreateResponse response = contentCreateService.createContent(request, accessToken);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/contents/{contentId}")
    public ResponseEntity<ContentDetailResponse> getContent(@PathVariable Long contentId) {
        ContentDetailResponse response = contentViewService.getContentById(contentId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/contents/{contentId}")
    public ResponseEntity<ContentUpdateResponse> updateContent(
            @PathVariable Long contentId,
            @RequestHeader("Authorization") String authorization,
            @ModelAttribute ContentUpdateRequest request) throws IOException {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ContentUpdateResponse errorResponse = new ContentUpdateResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String accessToken = authorization.substring(7);
        ContentUpdateResponse response = contentUpdateService.updateContent(contentId, request, accessToken);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/contents/{contentId}")
    public ResponseEntity<ContentDeleteResponse> deleteContent(
            @PathVariable Long contentId,
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ContentDeleteResponse errorResponse = new ContentDeleteResponse(false, "인증 토큰이 필요합니다");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String accessToken = authorization.substring(7);
        ContentDeleteResponse response = contentDeleteService.deleteContent(contentId, accessToken);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/contents/search")
    public ResponseEntity<ContentSearchResponse> searchContents(
            @RequestParam String q,
            @RequestParam(required = false) String postType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        if (q == null || q.trim().isEmpty()) {
            ContentSearchResponse errorResponse = new ContentSearchResponse(false, "검색어를 입력해주세요");
            return ResponseEntity.status(400).body(errorResponse);
        }
        
        PostType postTypeEnum = null;
        if (postType != null && !postType.trim().isEmpty()) {
            try {
                postTypeEnum = PostType.valueOf(postType.toUpperCase());
            } catch (IllegalArgumentException e) {
                ContentSearchResponse errorResponse = new ContentSearchResponse(false, "유효하지 않은 게시글 타입입니다");
                return ResponseEntity.status(400).body(errorResponse);
            }
        }
        
        ContentSearchRequest request = new ContentSearchRequest();
        request.setQ(q);
        request.setPostType(postTypeEnum);
        request.setPage(page);
        request.setSize(size);
        
        ContentSearchResponse response = contentSearchService.searchContents(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/contents")
    public ResponseEntity<ContentListResponse> getContentsByDate(
            @RequestParam String date,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String postType) {
        
        if (date == null || date.trim().isEmpty()) {
            ContentListResponse errorResponse = new ContentListResponse(false, "조회할 날짜를 입력해주세요");
            return ResponseEntity.status(400).body(errorResponse);
        }
        
        PostType postTypeEnum = null;
        if (postType != null && !postType.trim().isEmpty()) {
            try {
                postTypeEnum = PostType.valueOf(postType.toUpperCase());
            } catch (IllegalArgumentException e) {
                ContentListResponse errorResponse = new ContentListResponse(false, "유효하지 않은 게시글 타입입니다");
                return ResponseEntity.status(400).body(errorResponse);
            }
        }
        
        ContentListRequest request = new ContentListRequest();
        request.setDate(date);
        request.setLat(lat);
        request.setLng(lng);
        request.setRadius(radius);
        request.setPostType(postTypeEnum);
        
        ContentListResponse response = contentListService.getContentsByDate(request);
        
        return ResponseEntity.ok(response);
    }
}