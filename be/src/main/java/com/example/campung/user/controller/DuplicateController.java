package com.example.campung.user.controller;

import com.example.campung.user.dto.DuplicateCheckRequest;
import com.example.campung.user.dto.DuplicateCheckResponse;
import com.example.campung.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "인증 및 사용자 관련 API")
public class DuplicateController {

    @Autowired
    private UserService userService;

    @Operation(summary = "아이디/닉네임 중복 확인")
    @PostMapping("/duplicate")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicate(@RequestBody DuplicateCheckRequest request) {
        DuplicateCheckResponse response = userService.checkDuplicate(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
