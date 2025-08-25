package com.example.campung.friendship.service;

import com.example.campung.entity.*;
import com.example.campung.friendship.dto.*;
import com.example.campung.friendship.repository.FriendshipRepository;
import com.example.campung.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendshipService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendshipDto sendFriendRequest(FriendRequestDto requestDto) {
        // 요청자와 대상 사용자 존재 확인
        User requester = userRepository.findByUserId(requestDto.getRequesterId())
                .orElseThrow(() -> new IllegalArgumentException("요청자를 찾을 수 없습니다."));

        User targetUser = userRepository.findByUserId(requestDto.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        // 자기 자신에게 친구 요청하는 경우 방지
        if (requester.getId().equals(targetUser.getId())) {
            throw new IllegalStateException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
        }

        // 이미 친구이거나 요청이 존재하는지 확인 (양방향 확인)
        Optional<Friendship> existingFriendship = friendshipRepository.findByRequesterAndAddressee(
                requester, targetUser);

        Optional<Friendship> reverseExistingFriendship = friendshipRepository.findByRequesterAndAddressee(
                targetUser, requester);

        if (existingFriendship.isPresent() || reverseExistingFriendship.isPresent()) {
            throw new IllegalStateException("이미 친구이거나 요청이 존재합니다.");
        }

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(targetUser)
                .status("pending")
                .build();

        friendshipRepository.save(friendship);

        return FriendshipDto.builder()
                .friendshipId(friendship.getFriendshipId())
                .userId(targetUser.getUserId())
                .nickname(targetUser.getNickname())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .build();
    }

    public FriendshipDto acceptFriendRequest(Long friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다."));

        // 현재 사용자 조회
        User currentUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 요청 수신자인지 확인
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("요청을 수락할 권한이 없습니다.");
        }

        // 요청 상태 확인
        if (!"pending".equals(friendship.getStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        // 상태 업데이트
        friendship.setStatus("accepted");
        friendshipRepository.save(friendship);

        return FriendshipDto.builder()
                .friendshipId(friendship.getFriendshipId())
                .userId(friendship.getRequester().getUserId())
                .nickname(friendship.getRequester().getNickname())
                .status(friendship.getStatus())
                .createdAt(friendship.getUpdatedAt())
                .build();
    }

    public void rejectFriendRequest(Long friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("친구 요청을 찾을 수 없습니다."));

        // 현재 사용자 조회
        User currentUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 요청 수신자인지 확인
        if (!friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("요청을 거절할 권한이 없습니다.");
        }

        // 요청 상태 확인
        if (!"pending".equals(friendship.getStatus())) {
            throw new IllegalStateException("이미 처리된 요청입니다.");
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getReceivedFriendRequests(String userId) {
        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Friendship> friendships = friendshipRepository.findByAddresseeAndStatus(user, "pending");

        return friendships.stream()
                .map(friendship -> FriendshipDto.builder()
                        .friendshipId(friendship.getFriendshipId())
                        .userId(friendship.getRequester().getUserId())
                        .nickname(friendship.getRequester().getNickname())
                        .status(friendship.getStatus())
                        .createdAt(friendship.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getSentFriendRequests(String userId) {
        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Friendship> friendships = friendshipRepository.findByRequesterAndStatus(user, "pending");

        return friendships.stream()
                .map(friendship -> FriendshipDto.builder()
                        .friendshipId(friendship.getFriendshipId())
                        .userId(friendship.getAddressee().getUserId())
                        .nickname(friendship.getAddressee().getNickname())
                        .status(friendship.getStatus())
                        .createdAt(friendship.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendshipDto> getFriendsList(String userId) {
        // 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Friendship> friendships = friendshipRepository.findAcceptedFriendshipsByUser(user);

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getRequester().getId().equals(user.getId())
                            ? friendship.getAddressee()
                            : friendship.getRequester();

                    return FriendshipDto.builder()
                            .friendshipId(friendship.getFriendshipId())
                            .userId(friend.getUserId())
                            .nickname(friend.getNickname())
                            .status(friendship.getStatus())
                            .createdAt(friendship.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public void removeFriend(Long friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("친구 관계를 찾을 수 없습니다."));

        // 현재 사용자 조회
        User currentUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 친구 관계의 당사자인지 확인
        if (!friendship.getRequester().getId().equals(currentUser.getId()) &&
                !friendship.getAddressee().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("친구 관계를 해제할 권한이 없습니다.");
        }

        // 승인된 친구 관계인지 확인
        if (!"accepted".equals(friendship.getStatus())) {
            throw new IllegalStateException("친구 관계가 아닙니다.");
        }

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(String userId1, String userId2) {
        User user1 = userRepository.findByUserId(userId1).orElse(null);
        User user2 = userRepository.findByUserId(userId2).orElse(null);

        if (user1 == null || user2 == null) {
            return false;
        }

        return friendshipRepository.findAcceptedFriendshipBetweenUsers(user1, user2).isPresent();
    }
}