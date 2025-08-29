package com.example.campung.content.service;

import com.example.campung.content.dto.ContentLikeResponse;
import com.example.campung.content.repository.ContentLikeRepository;
import com.example.campung.content.repository.ContentRepository;
import com.example.campung.user.repository.UserRepository;
import com.example.campung.notification.service.NotificationService;
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
    
    @Autowired
    private ContentHotTrackingService contentHotTrackingService;
    
    @Autowired
    private NotificationService notificationService;
    
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
            
            // Redis에서 좋아요 제거
            contentHotTrackingService.removeLike(contentId, accessToken);
        } else {
            // 좋아요 추가
            ContentLike newLike = ContentLike.builder()
                    .content(content)
                    .user(user)
                    .build();
            contentLikeRepository.save(newLike);
            isLiked = true;
            message = "좋아요가 추가되었습니다";
            
            // Redis에서 좋아요 추적
            contentHotTrackingService.trackLike(contentId, accessToken);
            
            // 좋아요 알림 전송 (본인이 작성한 게시글이 아닌 경우에만)
            if (!content.getAuthor().getUserId().equals(accessToken)) {
                sendLikeNotification(content, user);
            }
        }
        
        // 총 좋아요 수 조회
        int totalLikes = contentLikeRepository.countByContentId(contentId);
        
        // Content 테이블의 like_count 필드 업데이트
        contentRepository.updateLikeCount(contentId, totalLikes);
        
        // Redis에서 Hot 랭킹 업데이트
        long currentLikes24h = contentHotTrackingService.getLike24hCount(contentId);
        contentHotTrackingService.updateHotRanking(contentId, currentLikes24h);
        
        ContentLikeResponse.ContentLikeData data = new ContentLikeResponse.ContentLikeData(isLiked, totalLikes);
        return new ContentLikeResponse(true, message, data);
    }
    
    private void sendLikeNotification(Content content, User liker) {
        try {
            User postAuthor = content.getAuthor();
            String likerName = "익명";
            
            String contentTitle = content.getTitle();
            if (contentTitle.length() > 10) {
                contentTitle = contentTitle.substring(0, 10) + "...";
            }
            
            String message = likerName + " 님이 " + contentTitle + " 글을 좋아합니다.";
            String title = "좋아요 알림";
            String type = "normal";
            String data = "{\"contentId\":" + content.getContentId() + "}";
            
            notificationService.createNotification(postAuthor, type, title, message, data);
        } catch (Exception e) {
            System.err.println("좋아요 알림 전송 중 오류 발생: " + e.getMessage());
        }
    }
}