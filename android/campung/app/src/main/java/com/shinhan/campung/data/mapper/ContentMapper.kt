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
            userId = data.userId,
            author = data.author,
            location = data.location ?: com.shinhan.campung.data.model.Location(0.0, 0.0),
            postType = data.postType,
            postTypeName = ContentCategory.fromValue(data.postType).displayName,
            markerType = if (data.hotContent) "hot" else data.postType,
            contentScope = "",
            contentType = "",
            title = data.title,
            body = data.body,
            mediaFiles = data.mediaFiles,
            emotionTag = "",
            reactions = com.shinhan.campung.data.model.Reactions(data.likeCount, data.commentCount),
            createdAt = data.createdAt ?: "",
            expiresAt = null
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