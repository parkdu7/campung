package com.example.campung.user.controller;

import com.example.campung.user.dto.DeleteUserRequest;
import com.example.campung.user.dto.DeleteUserResponse;
import com.example.campung.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "인증 및 사용자 관련 API")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/user")
    public ResponseEntity<DeleteUserResponse> deleteUser(@RequestBody DeleteUserRequest request) {
        DeleteUserResponse res = userService.deleteUser(request);
        if (res.isSuccess()) return ResponseEntity.ok(res);
        return ResponseEntity.badRequest().body(res);
    }
}
