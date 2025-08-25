package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName

data class MapContentResponse(
    val success: Boolean,
    val message: String,
    val data: MapContentData
)

data class MapContentData(
    val contents: List<MapContent>,
    val records: List<Any>,
    val totalCount: Int,
    val hasMore: Boolean
)

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
)

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
    val thumbnail: String?
)

data class Reactions(
    val likes: Int,
    val comments: Int
)