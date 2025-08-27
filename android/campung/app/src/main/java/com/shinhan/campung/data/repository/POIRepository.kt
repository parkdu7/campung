package com.shinhan.campung.data.repository

import com.shinhan.campung.data.model.POIData
import com.shinhan.campung.data.remote.api.POIApi
import com.shinhan.campung.data.remote.response.POIResponse
import com.shinhan.campung.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class POIRepository @Inject constructor(
    private val poiApi: POIApi
) {
    
    /**
     * 근처 POI 조회
     */
    suspend fun getNearbyPOIs(
        latitude: Double,
        longitude: Double,
        radius: Int,
        category: String? = null,
        limit: Int? = 50
    ): Result<List<POIData>> = withContext(Dispatchers.IO) {
        try {
            val response = poiApi.getNearbyPOIs(latitude, longitude, radius, category, limit)
            if (response.success) {
                // POIItem을 POIData로 변환
                val poiDataList = response.data.map { item ->
                    POIData(
                        id = item.id,
                        name = item.name,
                        category = item.category,
                        address = "", // 백엔드에서 제공하지 않음
                        latitude = item.latitude,
                        longitude = item.longitude,
                        phone = null,
                        rating = null,
                        distance = null,
                        isOpen = null,
                        openHours = null,
                        thumbnailUrl = item.thumbnailUrl
                    )
                }
                Result.success(poiDataList)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 키워드 기반 POI 검색
     */
    suspend fun searchPOIs(
        keyword: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Int? = 5000
    ): Result<List<POIData>> = withContext(Dispatchers.IO) {
        try {
            val response = poiApi.searchPOIs(keyword, latitude, longitude, radius)
            if (response.success) {
                // POIItem을 POIData로 변환
                val poiDataList = response.data.map { item ->
                    POIData(
                        id = item.id,
                        name = item.name,
                        category = item.category,
                        address = "", // 백엔드에서 제공하지 않음
                        latitude = item.latitude,
                        longitude = item.longitude,
                        phone = null,
                        rating = null,
                        distance = null,
                        isOpen = null,
                        openHours = null,
                        thumbnailUrl = item.thumbnailUrl
                    )
                }
                Result.success(poiDataList)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 사용 가능한 카테고리 목록 조회
     */
    suspend fun getAvailableCategories(
        latitude: Double,
        longitude: Double,
        radius: Int = 2000
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = poiApi.getNearbyPOIs(latitude, longitude, radius, null, 1)
            if (response.success) {
                // 응답에서 고유한 카테고리 목록 추출
                val categories = response.data.map { it.category }.distinct()
                Result.success(categories)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}