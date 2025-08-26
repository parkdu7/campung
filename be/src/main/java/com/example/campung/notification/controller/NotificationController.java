package com.example.campung.notification.controller;

import com.example.campung.notification.dto.NotificationListResponse;
import com.example.campung.notification.dto.NotificationSettingsRequest;
import com.example.campung.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "알림 관리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이지별로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다")
    })
    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        log.info("Getting notifications for user: {}, page: {}, size: {}", accessToken, page, size);

        NotificationListResponse response = notificationService.getNotifications(accessToken, page, size);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음으로 표시합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        log.info("Marking notification as read: notificationId={}, userId={}", notificationId, accessToken);

        notificationService.markAsRead(accessToken, notificationId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "알림 ID") @PathVariable Long notificationId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        log.info("Deleting notification: notificationId={}, userId={}", notificationId, accessToken);

        notificationService.deleteNotification(accessToken, notificationId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "알림 설정 업데이트", description = "사용자의 알림 설정을 업데이트합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 업데이트 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PutMapping("/settings")
    public ResponseEntity<Void> updateNotificationSettings(
            @RequestHeader("Authorization") String authorization,
            @RequestBody NotificationSettingsRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        log.info("Updating notification settings for user: {}", accessToken);

        notificationService.updateNotificationSettings(accessToken, request);

        return ResponseEntity.ok().build();
    }
}