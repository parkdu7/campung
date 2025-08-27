package com.example.campung.user.controller;

import com.example.campung.user.dto.LogoutResponse;
import com.example.campung.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "인증 및 사용자 관련 API")
public class LogoutController {

    @Autowired
    private UserService userService;

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        // 토큰 관리가 없다면 단순 성공 응답
        return ResponseEntity.ok(userService.logout());
    }
}
