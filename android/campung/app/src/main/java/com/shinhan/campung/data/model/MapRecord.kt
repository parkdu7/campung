package com.shinhan.campung.data.model

import com.google.gson.annotations.SerializedName

data class MapRecord(
    val recordId: Long,
    val userId: String,
    val author: Author,
    val location: Location,
    val recordUrl: String,
    val createdAt: String
) {
    val latitude: Double get() = location.latitude
    val longitude: Double get() = location.longitude
    val markerType: String get() = "marker_record"
}