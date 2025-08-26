package com.shinhan.campung.data.mapper

import com.shinhan.campung.data.model.ContentData
import com.shinhan.campung.data.model.ContentCategory
import com.shinhan.campung.data.model.MapContent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ContentMapper @Inject constructor() {
    
    fun toMapContent(data: ContentData): MapContent {
        return MapContent(
            contentId = data.contentId,
            title = data.title,
            content = data.body,
            authorNickname = if (data.author.anonymous) "익명" else data.author.nickname,
            category = ContentCategory.fromValue(data.postType),
            thumbnailUrl = data.mediaFiles.firstOrNull()?.thumbnailUrl,
            latitude = data.location?.latitude ?: 0.0,
            longitude = data.location?.longitude ?: 0.0,
            likeCount = data.likeCount,
            commentCount = data.commentCount,
            createdAt = parseDateTime(data.createdAt),
            isHot = data.hotContent
        )
    }
    
    private fun parseDateTime(dateStr: String?): LocalDateTime {
        return try {
            if (dateStr != null) {
                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            } else {
                LocalDateTime.now()
            }
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}