package com.example.Campung.User.Service;

import com.example.Campung.User.Dto.LoginRequest;
import com.example.Campung.User.Dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    public LoginResponse login(LoginRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return new LoginResponse(false, "사용자 ID를 입력해주세요");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return new LoginResponse(false, "비밀번호를 입력해주세요");
        }
        
        String accessToken = request.getUserId();
        return new LoginResponse(true, "로그인에 성공했습니다", accessToken);
    }
}