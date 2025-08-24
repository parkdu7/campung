package com.example.Campung.User.Controller;

import com.example.Campung.User.Dto.LogoutResponse;
import com.example.Campung.User.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LogoutController {

    @Autowired
    private UserService userService;

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        // 토큰 관리가 없다면 단순 성공 응답
        return ResponseEntity.ok(userService.logout());
    }
}
