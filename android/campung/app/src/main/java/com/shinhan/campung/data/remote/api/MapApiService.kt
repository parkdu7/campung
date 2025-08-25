package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.model.ContentResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MapApiService {
    @GET("/api/contents/{contentId}")
    suspend fun getContent(@Path("contentId") contentId: Long): ContentResponse
}