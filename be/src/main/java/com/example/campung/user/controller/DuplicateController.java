package com.example.campung.user.controller;

import com.example.campung.user.dto.DuplicateCheckRequest;
import com.example.campung.user.dto.DuplicateCheckResponse;
import com.example.campung.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DuplicateController {

    @Autowired
    private UserService userService;

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
