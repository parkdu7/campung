package com.example.Campung.User.service;

import com.example.Campung.User.dto.*;
import com.example.Campung.User.repository.UserRepository;
import com.example.Campung.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
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

    // 회원가입
    public SignupResponse signup(SignupRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return new SignupResponse(false, "사용자 ID를 입력해주세요");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return new SignupResponse(false, "비밀번호를 입력해주세요");
        }
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            return new SignupResponse(false, "닉네임을 입력해주세요");
        }
        if (userRepository.existsByUserId(request.getUserId().trim())) {
            return new SignupResponse(false, "이미 사용 중인 사용자 ID입니다");
        }

        String passwordHash = sha256Base64(request.getPassword().trim());

        User user = User.builder()
                .userId(request.getUserId().trim())
                .passwordHash(passwordHash)
                .nickname(request.getNickname().trim())
                .build();

        userRepository.save(user);

        String accessToken = request.getUserId().trim();
        return new SignupResponse(true, "회원가입에 성공했습니다", accessToken);
    }

    // 아이디 중복 체크
    public DuplicateCheckResponse checkDuplicate(DuplicateCheckRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return new DuplicateCheckResponse(false, "사용자 ID를 입력해주세요", false);
        }
        boolean exists = userRepository.existsByUserId(request.getUserId().trim());
        if (exists) {
            return new DuplicateCheckResponse(true, "이미 사용 중인 사용자 ID입니다", false);
        } else {
            return new DuplicateCheckResponse(true, "사용 가능한 사용자 ID입니다", true);
        }
    }

    // 로그아웃
    public LogoutResponse logout() {
        // 실제로 토큰 사용 시: 블랙리스트/만료 처리 등 수행
        return new LogoutResponse(true, "로그아웃되었습니다");
    }

    // 회원탈퇴
    public DeleteUserResponse deleteUser(DeleteUserRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            return new DeleteUserResponse(false, "사용자 ID를 입력해주세요");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return new DeleteUserResponse(false, "비밀번호를 입력해주세요");
        }

        Optional<User> opt = userRepository.findByUserId(request.getUserId().trim());
        if (opt.isEmpty()) {
            return new DeleteUserResponse(false, "존재하지 않는 사용자입니다");
        }

        User user = opt.get();
        String reqHash = sha256Base64(request.getPassword().trim());
        if (!reqHash.equals(user.getPasswordHash())) {
            return new DeleteUserResponse(false, "비밀번호가 일치하지 않습니다");
        }

        // 연관 엔티티가 많으므로 실제 운영에선 외래키/고아 제거 설정 확인 권장
        userRepository.delete(user);
        return new DeleteUserResponse(true, "회원탈퇴가 완료되었습니다");
    }


    private String sha256Base64(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            // 문제가 있으면 원문 저장을 피하고 에러 반환
            throw new RuntimeException("비밀번호 해시에 실패했습니다", e);
        }
    }
}