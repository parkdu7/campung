package com.example.campung.content.service;

import com.example.campung.content.dto.ContentLikeResponse;
import com.example.campung.content.repository.ContentLikeRepository;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.global.exception.ContentNotFoundException;
import com.example.campung.entity.Content;
import com.example.campung.entity.ContentLike;
import com.example.campung.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ContentLikeService {
    
    @Autowired
    private ContentLikeRepository contentLikeRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public ContentLikeResponse toggleLike(Long contentId, String accessToken) {
        // 게시글 존재 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));
        
        // 사용자 조회 또는 생성
        User user = userRepository.findByUserId(accessToken)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(accessToken)
                            .nickname(accessToken)
                            .passwordHash("temp_hash")
                            .build();
                    return userRepository.save(newUser);
                });
        
        // 기존 좋아요 확인
        Optional<ContentLike> existingLike = contentLikeRepository.findByContentContentIdAndUserUserId(contentId, accessToken);
        
        boolean isLiked;
        String message;
        
        if (existingLike.isPresent()) {
            // 좋아요 취소
            contentLikeRepository.delete(existingLike.get());
            isLiked = false;
            message = "좋아요가 취소되었습니다";
        } else {
            // 좋아요 추가
            ContentLike newLike = ContentLike.builder()
                    .content(content)
                    .user(user)
                    .build();
            contentLikeRepository.save(newLike);
            isLiked = true;
            message = "좋아요가 추가되었습니다";
        }
        
        // 총 좋아요 수 조회
        int totalLikes = contentLikeRepository.countByContentId(contentId);
        
        ContentLikeResponse.ContentLikeData data = new ContentLikeResponse.ContentLikeData(isLiked, totalLikes);
        return new ContentLikeResponse(true, message, data);
    }
}