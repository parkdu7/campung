package com.shinhan.campung.data.model

import com.google.gson.annotations.SerializedName
import com.shinhan.campung.R
import java.time.LocalDateTime

data class MapContent(
    val contentId: Long,
    val userId: String,
    val author: Author,
    val location: Location,
    val postType: String,
    val postTypeName: String,
    val markerType: String,
    val contentScope: String,
    val contentType: String,
    val title: String,
    val body: String,
    val mediaFiles: List<MediaFile>?,
    val emotionTag: String,
    val reactions: Reactions,
    val createdAt: String,
    val expiresAt: String?
) {
    // UI에서 사용하는 computed properties
    val content: String get() = body
    val authorNickname: String get() = if (author.anonymous) "익명" else author.nickname
    val category: ContentCategory get() = ContentCategory.fromValue(postType)
    val thumbnailUrl: String? get() = mediaFiles?.firstOrNull()?.thumbnail
    val latitude: Double get() = location.latitude
    val longitude: Double get() = location.longitude
    val likeCount: Int get() = reactions.likes
    val commentCount: Int get() = reactions.comments
    val createdAtDateTime: LocalDateTime get() = parseDateTime(createdAt)
    val createdAtLocalDateTime: LocalDateTime get() = createdAtDateTime // backward compatibility
    val isHot: Boolean get() = markerType == "hot"
    
    private fun parseDateTime(dateStr: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateStr.replace("Z", ""))
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}

data class Author(
    val nickname: String,
    val anonymous: Boolean
)

data class Location(
    val latitude: Double,
    val longitude: Double
)

data class MediaFile(
    val id: Long,
    val url: String,
    val type: String,
    val thumbnail: String?,
    val thumbnailUrl: String? = thumbnail // ContentResponse와의 호환성을 위해
)

data class Reactions(
    val likes: Int,
    val comments: Int
)

enum class ContentCategory(val value: String, val iconRes: Int, val displayName: String) {
    INFO("info", R.drawable.ic_info, "정보"),
    PROMOTION("promotion", R.drawable.ic_promotion, "홍보"),
    FREE("free", R.drawable.ic_free, "자유"),
    MARKET("market", R.drawable.ic_market, "장터"),
    HOT("hot", R.drawable.ic_hot, "인기");
    
    companion object {
        fun fromValue(value: String): ContentCategory {
            return values().find { it.value == value } ?: FREE
        }
    }
}