package com.shinhan.campung.data.remote.response

import com.google.gson.annotations.SerializedName
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord

data class MapContentResponse(
    val success: Boolean,
    val message: String,
    val data: MapContentData
)

data class MapContentData(
    val contents: List<MapContent>,
    val records: List<MapRecord>,
    val totalCount: Int,
    val hasMore: Boolean,
    val emotionWeather: String?,
    val emotionTemperature: Double?
)