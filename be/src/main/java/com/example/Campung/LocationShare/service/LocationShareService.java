package com.example.Campung.LocationShare.service;

import com.example.Campung.LocationShare.dto.*;
import com.example.Campung.LocationShare.repository.LocationRequestRepository;
import com.example.Campung.LocationShare.repository.LocationShareRepository;
import com.example.Campung.User.repository.UserRepository;
import com.example.Campung.entity.*;
import com.example.Campung.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LocationShareService {
    
    private final LocationRequestRepository locationRequestRepository;
    private final LocationShareRepository locationShareRepository;
    private final UserRepository userRepository;
    private final FCMService fcmService;
    private final NotificationRepository notificationRepository;
    
    @Transactional
    public LocationShareResponseDto requestLocationShare(String requesterUserId, LocationShareRequestDto request) {
        User fromUser = userRepository.findByUserId(requesterUserId)
                .orElseThrow(() -> new RuntimeException("요청자를 찾을 수 없습니다"));
        
        int successCount = 0;
        int totalCount = request.getFriendIds().size();
        
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24); // 24시간 후 만료
        
        for (Long friendId : request.getFriendIds()) {
            try {
                User toUser = userRepository.findById(friendId)
                        .orElseThrow(() -> new RuntimeException("친구를 찾을 수 없습니다: " + friendId));
                
                // LocationRequest 생성
                LocationRequest locationRequest = LocationRequest.builder()
                        .fromUser(fromUser)
                        .toUser(toUser)
                        .message(request.getPurpose())
                        .status("pending")
                        .expiresAt(expiresAt)
                        .build();
                
                LocationRequest savedRequest = locationRequestRepository.save(locationRequest);
                
                // 알림 저장
                Notification notification = Notification.builder()
                        .user(toUser)
                        .type("location_share_request")
                        .title("위치 공유 요청")
                        .message(fromUser.getNickname() + "님이 위치를 요청했습니다: " + request.getPurpose())
                        .data("{\"shareRequestId\":" + savedRequest.getLocationRequestId() + ",\"fromUserId\":\"" + requesterUserId + "\"}")
                        .isRead(false)
                        .build();
                
                notificationRepository.save(notification);
                
                // FCM 푸시 발송
                if (toUser.getFcmToken() != null && !toUser.getFcmToken().trim().isEmpty()) {
                    fcmService.sendLocationShareRequest(
                            toUser.getFcmToken(),
                            fromUser.getNickname(),
                            request.getPurpose(),
                            savedRequest.getLocationRequestId()
                    );
                }
                
                successCount++;
                log.info("Location share request sent successfully to user: {}", friendId);
                
            } catch (Exception e) {
                log.error("Failed to send location share request to user: {}", friendId, e);
            }
        }
        
        return LocationShareResponseDto.builder()
                .message(successCount + "명에게 위치 공유 요청을 보냈습니다")
                .successCount(successCount)
                .totalCount(totalCount)
                .build();
    }
    
    @Transactional
    public LocationShareRespondResponseDto respondToLocationShare(String respondentUserId, Long shareRequestId, LocationShareRespondDto response) {
        User respondent = userRepository.findByUserId(respondentUserId)
                .orElseThrow(() -> new RuntimeException("응답자를 찾을 수 없습니다"));
        
        LocationRequest locationRequest = locationRequestRepository.findByLocationRequestIdAndToUser(shareRequestId, respondent)
                .orElseThrow(() -> new RuntimeException("위치 공유 요청을 찾을 수 없습니다"));
        
        if (!"pending".equals(locationRequest.getStatus())) {
            throw new RuntimeException("이미 응답한 요청입니다");
        }
        
        if (locationRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 요청입니다");
        }
        
        if ("accept".equals(response.getAction())) {
            // 수락 처리
            if (response.getLatitude() == null || response.getLongitude() == null) {
                throw new RuntimeException("위치 정보가 필요합니다");
            }
            
            locationRequest.setStatus("accepted");
            locationRequest.setRespondedAt(LocalDateTime.now());
            
            // 위치 공유 기간 설정 (5분 고정)
            LocalDateTime displayUntil = LocalDateTime.now().plusMinutes(5);
            
            // LocationShare 생성
            LocationShare locationShare = LocationShare.builder()
                    .locationRequest(locationRequest)
                    .sharedLatitude(response.getLatitude())
                    .sharedLongitude(response.getLongitude())
                    .message(null)
                    .displayUntil(displayUntil)
                    .build();
            
            locationShareRepository.save(locationShare);
            
            // 요청자에게 FCM 푸시 발송 (위치 정보 포함)
            User requester = locationRequest.getFromUser();
            if (requester.getFcmToken() != null && !requester.getFcmToken().trim().isEmpty()) {
                fcmService.sendLocationShared(
                        requester.getFcmToken(),
                        respondent.getNickname(),
                        response.getLatitude(),
                        response.getLongitude(),
                        null,
                        locationShare.getLocationShareId(),
                        displayUntil
                );
            }
            
            // 요청자에게 알림 저장
            Notification notification = Notification.builder()
                    .user(requester)
                    .type("location_share_accepted")
                    .title("위치가 공유되었습니다")
                    .message(respondent.getNickname() + "님이 위치를 공유했습니다")
                    .data("{\"shareId\":" + locationShare.getLocationShareId() + ",\"latitude\":" + response.getLatitude() + ",\"longitude\":" + response.getLongitude() + ",\"userName\":\"" + respondent.getNickname() + "\"}")
                    .isRead(false)
                    .build();
            
            notificationRepository.save(notification);
            
            log.info("Location share accepted: requestId={}, respondent={}", shareRequestId, respondentUserId);
            
            return LocationShareRespondResponseDto.builder()
                    .message("위치를 공유했습니다")
                    .status("accepted")
                    .build();
                    
        } else if ("reject".equals(response.getAction())) {
            // 거절 처리
            locationRequest.setStatus("rejected");
            locationRequest.setRespondedAt(LocalDateTime.now());
            
            // 요청자에게 FCM 푸시 발송
            User requester = locationRequest.getFromUser();
            if (requester.getFcmToken() != null && !requester.getFcmToken().trim().isEmpty()) {
                fcmService.sendLocationShareRejected(
                        requester.getFcmToken(),
                        respondent.getNickname(),
                        null
                );
            }
            
            // 요청자에게 알림 저장
            Notification notification = Notification.builder()
                    .user(requester)
                    .type("location_share_rejected")
                    .title("위치 공유가 거절되었습니다")
                    .message(respondent.getNickname() + "님이 위치 공유를 거절했습니다")
                    .data("{\"userName\":\"" + respondent.getNickname() + "\"}")
                    .isRead(false)
                    .build();
            
            notificationRepository.save(notification);
            
            log.info("Location share rejected: requestId={}, respondent={}", shareRequestId, respondentUserId);
            
            return LocationShareRespondResponseDto.builder()
                    .message("위치 공유를 거절했습니다")
                    .status("rejected")
                    .build();
        } else {
            throw new RuntimeException("올바르지 않은 응답입니다");
        }
    }
}