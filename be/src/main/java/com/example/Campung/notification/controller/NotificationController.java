package com.example.Campung.notification.controller;

import com.example.Campung.notification.dto.NotificationListResponse;
import com.example.Campung.notification.dto.NotificationSettingsRequest;
import com.example.Campung.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestHeader("Authorization") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting notifications for user: {}, page: {}, size: {}", userId, page, size);
        
        NotificationListResponse response = notificationService.getNotifications(userId, page, size);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("Authorization") String userId,
            @PathVariable Long notificationId) {
        
        log.info("Marking notification as read: notificationId={}, userId={}", notificationId, userId);
        
        notificationService.markAsRead(userId, notificationId);
        
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @RequestHeader("Authorization") String userId,
            @PathVariable Long notificationId) {
        
        log.info("Deleting notification: notificationId={}, userId={}", notificationId, userId);
        
        notificationService.deleteNotification(userId, notificationId);
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/settings")
    public ResponseEntity<Void> updateNotificationSettings(
            @RequestHeader("Authorization") String userId,
            @RequestBody NotificationSettingsRequest request) {
        
        log.info("Updating notification settings for user: {}", userId);
        
        notificationService.updateNotificationSettings(userId, request);
        
        return ResponseEntity.ok().build();
    }
}