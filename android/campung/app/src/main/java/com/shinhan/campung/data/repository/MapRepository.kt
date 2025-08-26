package com.shinhan.campung.data.repository

import com.shinhan.campung.data.remote.api.MapApi
import com.shinhan.campung.data.remote.response.MapContentResponse

class MapRepository(
    private val api: MapApi
) {
    suspend fun getMapContents(
        latitude: Double,
        longitude: Double,
        radius: Int? = null,
        postType: String? = null,
        date: String? = null
    ): Result<MapContentResponse> = runCatching {
        api.getMapContents(latitude, longitude, radius, postType, date)
    }
}