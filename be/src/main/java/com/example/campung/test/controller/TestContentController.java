package com.example.campung.test.controller;

import com.example.campung.test.dto.TestContentRequest;
import com.example.campung.test.dto.TestContentResponse;
import com.example.campung.test.service.TestContentService;
import com.example.campung.test.service.AllContentDeleteService;
import com.example.campung.global.enums.EmotionTestType;
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
        description = "선택한 감정 타입에 따른 JSON 파일을 읽어 테스트 컨텐츠를 생성합니다. " +
                     "각 감정별로 10개씩 준비된 데이터를 사용하며, emotion은 GPT가 자동 분석합니다.\n\n" +
                     "**사용 가능한 감정 타입:**\n" +
                     "• DEPRESS: 우울한 분위기 (우울, 슬픔, 외로움 등)\n" +
                     "• BRIGHT: 밝은 분위기 (기쁨, 행복, 성취감 등)\n" +
                     "• NON_EMOTION_INFO: 정보 전달 (공지사항, 안내 등)\n" +
                     "• ANGRY: 화난 분위기 (분노, 짜증, 불만 등)\n" +
                     "• EXCITE: 흥분한 분위기 (설렘, 기대감, 열정 등)\n" +
                     "• RANDOM: 무작위 분위기 (일상적, 중성적 내용)\n" +
                     "• ALL_MIXED: 전체 혼합 (기존 test.json의 모든 감정)"
    )
    @PostMapping("/contents")
    public ResponseEntity<TestContentResponse> createTestContents(@RequestBody TestContentRequest request) {
        log.info("=== 테스트 컨텐츠 생성 API 호출 ===");
        log.info("Request: lat={}, lng={}, category={}, userId={}, emotionType={}", 
            request.getLatitude(), request.getLongitude(), request.getCategory(), 
            request.getUserId(), request.getEmotionType());
        
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
        
        if (request.getEmotionType() == null) {
            TestContentResponse errorResponse = new TestContentResponse(false, "감정 타입은 필수입니다", 0);
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
    
    @Operation(
        summary = "감정 테스트 타입 목록 조회",
        description = "사용 가능한 모든 감정 테스트 타입과 설명을 조회합니다.\n\n" +
                     "각 타입별로 어떤 종류의 콘텐츠가 생성되는지 확인할 수 있습니다."
    )
    @GetMapping("/emotion-types")
    public ResponseEntity<java.util.Map<String, Object>> getEmotionTypes() {
        log.info("=== 감정 테스트 타입 목록 조회 API 호출 ===");
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        java.util.List<java.util.Map<String, String>> types = new java.util.ArrayList<>();
        
        for (EmotionTestType type : EmotionTestType.values()) {
            java.util.Map<String, String> typeInfo = new java.util.HashMap<>();
            typeInfo.put("type", type.name());
            typeInfo.put("fileName", type.getFileName());
            typeInfo.put("displayName", type.getDisplayName());
            typeInfo.put("description", type.getDescription());
            types.add(typeInfo);
        }
        
        response.put("success", true);
        response.put("message", "감정 테스트 타입 목록 조회 성공");
        response.put("emotionTypes", types);
        response.put("totalCount", types.size());
        
        return ResponseEntity.ok(response);
    }
}