package com.shinhan.campung.data.model

import com.shinhan.campung.R
import java.time.LocalDateTime

data class MapContent(
    val contentId: Long,
    val title: String,
    val content: String,
    val authorNickname: String,
    val category: ContentCategory,
    val thumbnailUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: LocalDateTime,
    val isHot: Boolean = false
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