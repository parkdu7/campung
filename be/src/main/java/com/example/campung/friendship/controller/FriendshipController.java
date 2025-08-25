package com.example.campung.friendship.controller;

import com.example.campung.friendship.dto.FriendRequestDto;
import com.example.campung.friendship.dto.FriendshipDto;
import com.example.campung.friendship.service.FriendshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friends", description = "친구 관리 API")
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냅니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 요청 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 친구이거나 요청이 존재함")
    })
    @PostMapping("/requests")
    public ResponseEntity<FriendshipDto> sendFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @RequestBody FriendRequestDto requestDto) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        // 현재 사용자 ID를 요청자로 설정
        requestDto.setRequesterId(accessToken);

        FriendshipDto response = friendshipService.sendFriendRequest(requestDto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 요청 수락 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "친구 요청을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "이미 처리된 요청")
    })
    @PutMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<FriendshipDto> acceptFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "친구 관계 ID") @PathVariable Long friendshipId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        FriendshipDto response = friendshipService.acceptFriendRequest(friendshipId, accessToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 요청 거절 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "친구 요청을 찾을 수 없음")
    })
    @PutMapping("/requests/{friendshipId}/reject")
    public ResponseEntity<String> rejectFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "친구 관계 ID") @PathVariable Long friendshipId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("인증 토큰이 필요합니다");
        }

        String accessToken = authorization.substring(7);
        friendshipService.rejectFriendRequest(friendshipId, accessToken);
        return ResponseEntity.ok("친구 요청이 거절되었습니다.");
    }

    @Operation(summary = "받은 친구 요청 목록 조회", description = "내가 받은 친구 요청 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다")
    })
    @GetMapping("/requests/received")
    public ResponseEntity<List<FriendshipDto>> getReceivedFriendRequests(
            @RequestHeader("Authorization") String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        List<FriendshipDto> requests = friendshipService.getReceivedFriendRequests(accessToken);
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "보낸 친구 요청 목록 조회", description = "내가 보낸 친구 요청 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다")
    })
    @GetMapping("/requests/sent")
    public ResponseEntity<List<FriendshipDto>> getSentFriendRequests(
            @RequestHeader("Authorization") String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        List<FriendshipDto> requests = friendshipService.getSentFriendRequests(accessToken);
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "친구 목록 조회", description = "내 친구 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다")
    })
    @GetMapping
    public ResponseEntity<List<FriendshipDto>> getFriendsList(
            @RequestHeader("Authorization") String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorization.substring(7);
        List<FriendshipDto> friends = friendshipService.getFriendsList(accessToken);
        return ResponseEntity.ok(friends);
    }

    @Operation(summary = "친구 끊기", description = "친구 관계를 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 끊기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 필요합니다"),
            @ApiResponse(responseCode = "404", description = "친구 관계를 찾을 수 없음")
    })
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<String> removeFriend(
            @RequestHeader("Authorization") String authorization,
            @Parameter(description = "친구 관계 ID") @PathVariable Long friendshipId) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("인증 토큰이 필요합니다");
        }

        String accessToken = authorization.substring(7);
        friendshipService.removeFriend(friendshipId, accessToken);
        return ResponseEntity.ok("친구 관계가 해제되었습니다.");
    }
}