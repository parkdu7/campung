package com.example.Campung.LocationShare.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {
    
    public void sendLocationShareRequest(String fcmToken, String fromUserName, String message, Long shareRequestId) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("FCM token is null or empty for location share request");
            return;
        }
        
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "location_share_request");
            data.put("shareRequestId", String.valueOf(shareRequestId));
            data.put("fromUserName", fromUserName);
            data.put("message", message);
            data.put("action_buttons", "true"); // Action Button 활성화 플래그
            
            Notification notification = Notification.builder()
                    .setTitle("위치 공유 요청")
                    .setBody(fromUserName + "님이 위치를 요청했습니다: " + message)
                    .build();
            
            // Android용 Action Button 설정
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setTitle("위치 공유 요청")
                            .setBody(fromUserName + "님이 위치를 요청했습니다: " + message)
                            .setChannelId("location_share_channel")
                            .build())
                    .build();
                    
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putAllData(data)
                    .setAndroidConfig(androidConfig)
                    .build();
                    
            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("Successfully sent location share request FCM: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to send location share request FCM to token: {}", fcmToken, e);
        }
    }
    
    public void sendLocationShared(String fcmToken, String userName, BigDecimal latitude, BigDecimal longitude, String message, Long shareId, LocalDateTime displayUntil) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("FCM token is null or empty for location share");
            return;
        }
        
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "location_share");
            data.put("shareId", String.valueOf(shareId));
            data.put("latitude", latitude.toString());
            data.put("longitude", longitude.toString());
            data.put("userName", userName);
            data.put("message", message != null ? message : "");
            data.put("displayUntil", displayUntil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Notification notification = Notification.builder()
                    .setTitle("위치가 공유되었습니다")
                    .setBody(userName + "님이 위치를 공유했습니다")
                    .build();
                    
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putAllData(data)
                    .build();
                    
            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("Successfully sent location share FCM: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to send location share FCM to token: {}", fcmToken, e);
        }
    }
    
    public void sendLocationShareRejected(String fcmToken, String userName, String message) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("FCM token is null or empty for location share rejection");
            return;
        }
        
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "location_share_rejected");
            data.put("userName", userName);
            data.put("message", message != null ? message : "");
            
            Notification notification = Notification.builder()
                    .setTitle("위치 공유가 거절되었습니다")
                    .setBody(userName + "님이 위치 공유를 거절했습니다")
                    .build();
                    
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putAllData(data)
                    .build();
                    
            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("Successfully sent location share rejection FCM: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to send location share rejection FCM to token: {}", fcmToken, e);
        }
    }
}