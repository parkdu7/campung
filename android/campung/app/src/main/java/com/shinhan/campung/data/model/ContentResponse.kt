package com.shinhan.campung.data.model

import com.google.gson.annotations.SerializedName

data class ContentResponse(
    val success: Boolean,
    val message: String,
    val data: ContentData
)

data class ContentData(
    val contentId: Long,
    val userId: String,
    val author: Author,
    val location: Location?,
    val postType: String,
    val title: String,
    val body: String,
    val mediaFiles: List<MediaFile>?,
    val likeInfo: LikeInfo,
    val hotContent: Boolean,
    val createdAt: String // API에서 제공하는 생성일시 (ISO 8601 형식)
) {
    // 호환성을 위한 computed properties - 기존 코드와의 호환성
    val likeCount: Int get() = likeInfo.totalLikes
    val commentCount: Int get() = 0 // 현재 API에서 제공하지 않음
    val reactions: Reactions get() = Reactions(likeInfo.totalLikes, 0, likeInfo.likedByCurrentUser)
    // createdAt은 실제 필드를 사용 (위에서 정의됨)
    val expiresAt: String? get() = null
    val postTypeName: String get() = when(postType) {
        "INFO" -> "정보"
        "PROMOTION" -> "홍보"
        "FREE" -> "자유"
        "MARKET" -> "장터"
        else -> postType
    }
    val markerType: String get() = if (hotContent) "hot" else postType.lowercase()
    val contentScope: String get() = "MAP"
    val contentType: String get() = "TEXT"
    val emotionTag: String get() = ""
}

// 실제 API 응답에 맞는 새로운 데이터 클래스들
data class LikeInfo(
    val totalLikes: Int,
    val likedByCurrentUser: Boolean
)

