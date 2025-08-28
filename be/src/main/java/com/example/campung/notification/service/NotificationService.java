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
import com.example.campung.locationShare.service.FCMService;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;
    
    public NotificationListResponse getNotifications(String userId, int page, int size) {
        // 사용자 존재 확인
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다"));
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 읽지 않은 알림만 조회하도록 변경
        var notificationPage = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        long unreadCount = notificationPage.getTotalElements(); // 모든 결과가 읽지 않은 것이므로 totalElements 사용
        
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
        Notification notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found or access denied"));
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            log.info("Notification marked as read: notificationId={}, userId={}", notificationId, userId);
        }
    }
    
    @Transactional
    public void deleteNotification(String userId, Long notificationId) {
        Notification notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, userId)
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
    
    @Transactional
    public void createNotification(User targetUser, String type, String title, String message, String data) {
        Notification notification = Notification.builder()
                .user(targetUser)
                .type(type)
                .title(title)
                .message(message)
                .data(data)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        log.info("Notification created: type={}, title={}, userId={}", type, title, targetUser.getUserId());
        
        // FCM 푸시 알림 발송
        sendFCMNotification(targetUser.getFcmToken(), title, message, data);
    }
    
    private void sendFCMNotification(String fcmToken, String title, String message, String data) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("FCM token is null or empty, skipping push notification");
            return;
        }
        
        try {
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("type", "normal");
            if (data != null) {
                dataMap.put("data", data);
            }
            
            com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(message)
                    .build();
            
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .setChannelId("default_channel")
                            .build())
                    .build();
            
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putAllData(dataMap)
                    .setAndroidConfig(androidConfig)
                    .build();
            
            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("Successfully sent FCM notification: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to send FCM notification to token: {}", fcmToken, e);
        }
    }
}