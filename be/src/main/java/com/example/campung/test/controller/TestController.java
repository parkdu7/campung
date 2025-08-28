package com.example.campung.test.controller;

import com.example.campung.content.service.ContentLikeService;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Tag(name = "Test", description = "테스트용 API")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ContentLikeService contentLikeService;
    
    @Operation(summary = "테스트 사용자 회원가입", description = "testuser1부터 testuser10까지 자동으로 회원가입시킵니다.")
    @PostMapping("/signUp")
    public ResponseEntity<Map<String, Object>> signUp() {
        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        
        try {
            for (int i = 1; i <= 10; i++) {
                String userId = "testuser" + i;
                String nickname = "테스트유저" + i;
                
                // 이미 존재하는 사용자인지 확인
                if (userRepository.findByUserId(userId).isEmpty()) {
                    User user = User.builder()
                            .userId(userId)
                            .nickname(nickname)
                            .passwordHash("test_hash_" + i)
                            .fcmToken("test_fcm_token_" + i)
                            .build();
                    
                    userRepository.save(user);
                    successCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "테스트 사용자 " + successCount + "명이 생성되었습니다.");
            response.put("createdUsers", successCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "테스트 사용자 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @Operation(summary = "좋아요 테스트", description = "testuser1부터 testuser10까지 지정된 게시글에 좋아요를 누릅니다.")
    @PostMapping("/likeTest")
    public ResponseEntity<Map<String, Object>> likeTest(
            @Parameter(description = "좋아요를 누를 게시글 ID", required = true)
            @RequestParam Long contentId) {
        
        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        try {
            for (int i = 1; i <= 10; i++) {
                String userId = "testuser" + i;
                
                try {
                    contentLikeService.toggleLike(contentId, userId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    System.err.println("좋아요 실패 - " + userId + ": " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("message", "좋아요 테스트 완료");
            response.put("contentId", contentId);
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "좋아요 테스트 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}