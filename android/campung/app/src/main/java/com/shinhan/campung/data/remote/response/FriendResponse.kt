package com.shinhan.campung.data.remote.response

data class FriendResponse(
    val friendshipId: Long,      // 친구 관계 ID
    val userId: String,          // 상대방 사용자 ID
    val nickname: String,        // 상대방 닉네임
    val status: String,          // "pending", "accepted"
    val createdAt: String?       // 생성 시간 (ISO 8601 문자열)
)