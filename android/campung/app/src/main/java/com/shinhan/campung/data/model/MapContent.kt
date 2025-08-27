package com.shinhan.campung.data.model

import com.google.gson.annotations.SerializedName
import com.shinhan.campung.R
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.ZoneId

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
            // Z가 없는 경우 UTC로 간주하고 Z 추가
            val normalizedDateStr = if (dateStr.endsWith("Z")) dateStr else "${dateStr}Z"
            // UTC 시간을 한국 시간으로 변환
            val zonedDateTime = ZonedDateTime.parse(normalizedDateStr)
            zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        } catch (e: Exception) {
            // 한국 현재 시간으로 fallback
            ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        }
    }

    override fun hashCode(): Int {
        var result = contentId.hashCode()
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + author.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + (postType?.hashCode() ?: 0)
        result = 31 * result + (postTypeName?.hashCode() ?: 0)
        result = 31 * result + (markerType?.hashCode() ?: 0)
        result = 31 * result + (contentScope?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + (mediaFiles?.hashCode() ?: 0)
        result = 31 * result + (emotionTag?.hashCode() ?: 0)
        result = 31 * result + reactions.hashCode()
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        return result
    }
}



data class Author(
    val nickname: String,
    val profileImageUrl: String? = null,
    val isAnonymous: Boolean = false
) {
    // 기존 코드와의 호환성을 위한 computed property
    val anonymous: Boolean get() = isAnonymous
}

data class Location(
    val latitude: Double,
    val longitude: Double
)

data class MediaFile(
    val fileId: Long,
    val fileType: String,
    val fileUrl: String,
    val thumbnailUrl: String?,
    val fileName: String?,
    val fileSize: Long?,
    val order: Int?
) {
    // 기존 코드와의 호환성을 위한 computed properties
    val id: Long get() = fileId
    val url: String get() = fileUrl
    val type: String get() = fileType
    val thumbnail: String? get() = thumbnailUrl
    
    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + (fileType?.hashCode() ?: 0)
        result = 31 * result + (fileUrl?.hashCode() ?: 0)
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (fileSize?.hashCode() ?: 0)
        result = 31 * result + (order?.hashCode() ?: 0)
        return result
    }
}

data class Reactions(
    val likes: Int,
    val comments: Int,
    val isLiked: Boolean = false // 현재 사용자의 좋아요 여부
)

data class Comment(
    val commentId: Long,
    val userId: String,
    val author: Author,
    val body: String,
    val mediaFiles: List<MediaFile>?,
    val createdAt: String,
    val replies: List<Reply>?,
    val replyCount: Int
)

data class Reply(
    val replyId: Long,
    val userId: String,
    val author: Author,
    val body: String,
    val mediaFiles: List<MediaFile>?,
    val createdAt: String
)

data class LikeResponse(
    val isLiked: Boolean,
    val totalLikes: Int
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