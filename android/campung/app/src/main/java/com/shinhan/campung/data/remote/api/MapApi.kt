package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.MapContentResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MapApi {
    @GET("map/contents")
    suspend fun getMapContents(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Int? = null,
        @Query("postType") postType: String? = null,
        @Query("date") date: String? = null
    ): MapContentResponse
}