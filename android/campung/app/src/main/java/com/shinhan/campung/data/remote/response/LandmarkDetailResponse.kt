package com.shinhan.campung.data.remote.response

data class LandmarkDetailResponse(
    val success: Boolean,
    val data: LandmarkDetailData
)

data class LandmarkDetailData(
    val id: Long,
    val name: String,
    val description: String?,
    val thumbnailUrl: String?,
    val imageUrl: String?,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val currentSummary: String?,
    val summaryUpdatedAt: String?,
    val createdAt: String
)