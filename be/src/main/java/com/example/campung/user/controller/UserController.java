package com.example.campung.user.controller;

import com.example.campung.user.dto.DeleteUserRequest;
import com.example.campung.user.dto.DeleteUserResponse;
import com.example.campung.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @DeleteMapping("/user")
    public ResponseEntity<DeleteUserResponse> deleteUser(@RequestBody DeleteUserRequest request) {
        DeleteUserResponse res = userService.deleteUser(request);
        if (res.isSuccess()) return ResponseEntity.ok(res);
        return ResponseEntity.badRequest().body(res);
    }
}
