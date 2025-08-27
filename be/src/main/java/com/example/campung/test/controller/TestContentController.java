package com.example.campung.test.controller;

import com.example.campung.test.dto.TestContentRequest;
import com.example.campung.test.dto.TestContentResponse;
import com.example.campung.test.service.TestContentService;
import com.example.campung.test.service.AllContentDeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Content", description = "테스트 컨텐츠 관리 API")
public class TestContentController {
    
    @Autowired
    private TestContentService testContentService;
    
    @Autowired
    private AllContentDeleteService allContentDeleteService;
    
    @Operation(
        summary = "테스트 컨텐츠 생성",
        description = "test.json 파일을 읽어 {숫자} 패턴에 따라 테스트 컨텐츠를 생성합니다. " +
                     "emotion은 설정하지 않고 GPT가 자동으로 분석하도록 합니다."
    )
    @PostMapping("/contents")
    public ResponseEntity<TestContentResponse> createTestContents(@RequestBody TestContentRequest request) {
        log.info("=== 테스트 컨텐츠 생성 API 호출 ===");
        log.info("Request: lat={}, lng={}, category={}, userId={}", 
            request.getLatitude(), request.getLongitude(), request.getCategory(), request.getUserId());
        
        // 요청 검증
        if (request.getLatitude() == null || request.getLongitude() == null) {
            TestContentResponse errorResponse = new TestContentResponse(false, "위도와 경도는 필수입니다", 0);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (request.getCategory() == null) {
            TestContentResponse errorResponse = new TestContentResponse(false, "카테고리는 필수입니다", 0);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            TestContentResponse errorResponse = new TestContentResponse(false, "사용자 ID는 필수입니다", 0);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        TestContentResponse response = testContentService.createTestContents(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Operation(
        summary = "모든 컨텐츠 삭제",
        description = "데이터베이스의 모든 컨텐츠를 삭제합니다. " +
                     "연관된 좋아요, 댓글, 첨부파일, 신고, ContentHot 데이터도 함께 삭제됩니다."
    )
    @DeleteMapping("/contents/all")
    public ResponseEntity<TestContentResponse> deleteAllContents() {
        log.info("=== 모든 컨텐츠 삭제 API 호출 ===");
        
        TestContentResponse response = allContentDeleteService.deleteAllContents();
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
}