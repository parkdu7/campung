package com.shinhan.campung.data.model

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
    val mediaFiles: List<MediaFile>,
    val hotContent: Boolean,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: String? = null // ISO 8601 format
)

