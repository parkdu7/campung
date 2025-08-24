package com.example.campung.notification.service;

import com.example.campung.entity.Notification;
import com.example.campung.entity.NotificationSetting;
import com.example.campung.entity.User;
import com.example.campung.notification.dto.NotificationListResponse;
import com.example.campung.notification.dto.NotificationResponse;
import com.example.campung.notification.dto.NotificationSettingsRequest;
import com.example.campung.notification.repository.NotificationRepository;
import com.example.campung.notification.repository.NotificationSettingRepository;
import com.example.campung.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    
    public NotificationListResponse getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        var notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        
        List<NotificationResponse> notifications = notificationPage.getContent()
                .stream()
                .map(NotificationResponse::from)
                .toList();
        
        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(unreadCount)
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .currentPage(page)
                .size(size)
                .build();
    }
    
    @Transactional
    public void markAsRead(String userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found or access denied"));
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            log.info("Notification marked as read: notificationId={}, userId={}", notificationId, userId);
        }
    }
    
    @Transactional
    public void deleteNotification(String userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found or access denied"));
        
        notificationRepository.delete(notification);
        log.info("Notification deleted: notificationId={}, userId={}", notificationId, userId);
    }
    
    @Transactional
    public void updateNotificationSettings(String userId, NotificationSettingsRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 설정이 없으면 새로 생성
                    NotificationSetting newSetting = NotificationSetting.builder()
                            .user(user)
                            .likes(true)
                            .comments(true)
                            .location(false)
                            .build();
                    return notificationSettingRepository.save(newSetting);
                });
        
        // 요청에서 null이 아닌 값들만 업데이트
        if (request.getLikes() != null) {
            setting.setLikes(request.getLikes());
        }
        if (request.getComments() != null) {
            setting.setComments(request.getComments());
        }
        if (request.getLocation() != null) {
            setting.setLocation(request.getLocation());
        }
        
        notificationSettingRepository.save(setting);
        log.info("Notification settings updated for user: {}", userId);
    }
}