package com.example.Campung.User.controller;

import com.example.Campung.User.dto.DeleteUserRequest;
import com.example.Campung.User.dto.DeleteUserResponse;
import com.example.Campung.User.service.UserService;
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
