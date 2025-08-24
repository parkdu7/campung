package com.example.campung.user.controller;

import com.example.campung.user.dto.SignupRequest;
import com.example.campung.user.dto.SignupResponse;
import com.example.campung.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SignupController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        SignupResponse response = userService.signup(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
