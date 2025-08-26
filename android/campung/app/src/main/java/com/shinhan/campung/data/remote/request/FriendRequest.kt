package com.shinhan.campung.data.remote.request

data class FriendRequest(
    var requesterId: String? = null, // 서버에서 토큰으로 설정
    val targetUserId: String        // 이메일이 아니라 userId
)