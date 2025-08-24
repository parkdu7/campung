package com.example.Campung.User.Controller;

import com.example.Campung.User.Dto.SignupRequest;
import com.example.Campung.User.Dto.SignupResponse;
import com.example.Campung.User.Service.UserService;
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
