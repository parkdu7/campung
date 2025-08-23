package com.example.Campung.User.controller;

import com.example.Campung.User.dto.DuplicateCheckRequest;
import com.example.Campung.User.dto.DuplicateCheckResponse;
import com.example.Campung.User.service.UserService;
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
