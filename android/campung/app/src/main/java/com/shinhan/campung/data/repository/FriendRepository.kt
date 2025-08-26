package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.remote.api.FriendApi
import com.shinhan.campung.data.remote.request.FriendRequest
import com.shinhan.campung.data.remote.response.FriendResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val friendApi: FriendApi,
    private val authDataStore: AuthDataStore
) {

    // 토큰 가져오기 헬퍼 메서드
    private suspend fun getAuthToken(): String {
        val token = authDataStore.tokenFlow.first()
            ?: throw IllegalStateException("인증 토큰이 없습니다. 다시 로그인해주세요.")
        return "Bearer $token"
    }

    // 친구 목록 조회
    suspend fun getFriendsList(): List<FriendResponse> {
        return friendApi.getFriendsList(getAuthToken())
    }

    // 친구 요청 보내기
    suspend fun sendFriendRequest(targetUserId: String): FriendResponse {
        val request = FriendRequest(targetUserId = targetUserId)
        return friendApi.sendFriendRequest(getAuthToken(), request)
    }

    // 친구 요청 수락
    suspend fun acceptFriendRequest(friendshipId: Long): FriendResponse {
        return friendApi.acceptFriendRequest(getAuthToken(), friendshipId)
    }

    // 친구 요청 거절
    suspend fun rejectFriendRequest(friendshipId: Long): String {
        return friendApi.rejectFriendRequest(getAuthToken(), friendshipId)
    }

    // 받은 친구 요청 목록 조회
    suspend fun getReceivedFriendRequests(): List<FriendResponse> {
        return friendApi.getReceivedFriendRequests(getAuthToken())
    }

    // 보낸 친구 요청 목록 조회
    suspend fun getSentFriendRequests(): List<FriendResponse> {
        return friendApi.getSentFriendRequests(getAuthToken())
    }

    // 친구 끊기
    suspend fun removeFriend(friendshipId: Long): String {
        return friendApi.removeFriend(getAuthToken(), friendshipId)
    }

    // 로컬에서 친구 검색 (네트워크 요청 없음)
    suspend fun searchFriends(query: String): List<FriendResponse> {
        val allFriends = getFriendsList()
        return if (query.isEmpty()) {
            allFriends
        } else {
            allFriends.filter { friend ->
                friend.nickname.contains(query, ignoreCase = true) ||
                        friend.userId.contains(query, ignoreCase = true)
            }
        }
    }

    // 특정 친구와의 관계 상태 확인
    suspend fun getFriendshipStatus(targetUserId: String): String? {
        return try {
            // 받은 요청에서 찾기
            val receivedRequests = getReceivedFriendRequests()
            receivedRequests.find { it.userId == targetUserId }?.status
                ?: run {
                    // 보낸 요청에서 찾기
                    val sentRequests = getSentFriendRequests()
                    sentRequests.find { it.userId == targetUserId }?.status
                        ?: run {
                            // 친구 목록에서 찾기
                            val friends = getFriendsList()
                            friends.find { it.userId == targetUserId }?.status
                        }
                }
        } catch (e: Exception) {
            null
        }
    }

    // 캐시 새로고침 (필요 시 구현)
    suspend fun refreshCache() {
        // 로컬 캐시가 있다면 여기서 새로고침
        // 현재는 API 호출만 하므로 특별한 처리 없음
    }
}

// 확장 함수들 (유틸리티)
suspend fun FriendRepository.isFriend(targetUserId: String): Boolean {
    return try {
        val friends = getFriendsList()
        friends.any { it.userId == targetUserId && it.status == "accepted" }
    } catch (e: Exception) {
        false
    }
}

suspend fun FriendRepository.hasPendingRequest(targetUserId: String): Boolean {
    return try {
        val status = getFriendshipStatus(targetUserId)
        status == "pending"
    } catch (e: Exception) {
        false
    }
}