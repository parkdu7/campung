package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.POIResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface POIApi {
    
    /**
     * 중심점 기반 POI 조회
     * @param latitude 중심점 위도
     * @param longitude 중심점 경도
     * @param radius 반경 (미터 단위)
     * @param category POI 카테고리 필터 (optional)
     * @param limit 조회할 최대 개수 (optional, default: 50)
     */
    @GET("landmark/map")
    suspend fun getNearbyPOIs(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Int,
        @Query("category") category: String? = null,
        @Query("limit") limit: Int? = 50
    ): POIResponse
    
    /**
     * 키워드 기반 POI 검색
     * @param keyword 검색 키워드
     * @param latitude 기준점 위도 (optional)
     * @param longitude 기준점 경도 (optional)
     * @param radius 검색 반경 (optional, default: 5000)
     */
    @GET("landmark/search")
    suspend fun searchPOIs(
        @Query("keyword") keyword: String,
        @Query("lat") latitude: Double? = null,
        @Query("lng") longitude: Double? = null,
        @Query("radius") radius: Int? = 5000
    ): POIResponse
}