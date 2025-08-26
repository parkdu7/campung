package com.example.campung.locationShare.controller;

import com.example.campung.locationShare.dto.*;
import com.example.campung.locationShare.service.LocationShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Location Share", description = "친구 위치 공유 API")
public class LocationShareController {
    
    private final LocationShareService locationShareService;
    
    @PostMapping("/share/request")
    @Operation(summary = "위치 공유 요청", description = "친구들에게 위치 공유를 요청합니다")
    public ResponseEntity<LocationShareResponseDto> requestLocationShare(
            @RequestHeader("Authorization") String userId,
            @RequestBody LocationShareRequestDto request) {
        
        log.info("Location share request from user: {}, to {} friends", userId, request.getFriendIds().size());
        
        try {
            LocationShareResponseDto response = locationShareService.requestLocationShare(userId, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to request location share: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(LocationShareResponseDto.builder()
                            .message("위치 공유 요청에 실패했습니다: " + e.getMessage())
                            .successCount(0)
                            .totalCount(request.getFriendIds().size())
                            .build());
        }
    }
    
    @PutMapping("/share/request/{shareRequestId}/respond")
    @Operation(summary = "위치 공유 응답", description = "위치 공유 요청에 수락 또는 거절로 응답합니다")
    public ResponseEntity<LocationShareRespondResponseDto> respondToLocationShare(
            @RequestHeader("Authorization") String userId,
            @PathVariable Long shareRequestId,
            @RequestBody LocationShareRespondDto response) {
        
        log.info("Location share response from user: {}, requestId: {}, action: {}", 
                userId, shareRequestId, response.getAction());
        
        try {
            LocationShareRespondResponseDto result = locationShareService.respondToLocationShare(
                    userId, shareRequestId, response);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to respond to location share: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(LocationShareRespondResponseDto.builder()
                            .message("응답에 실패했습니다: " + e.getMessage())
                            .status("error")
                            .build());
        }
    }
}