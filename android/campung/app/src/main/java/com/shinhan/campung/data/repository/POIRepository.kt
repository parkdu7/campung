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
                        thumbnailUrl = item.thumbnailUrl,
                        imageUrl = item.imageUrl, // imageUrl 추가
                        currentSummary = item.currentSummary ?: generateDummySummary(item.name, item.category)
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
                        thumbnailUrl = item.thumbnailUrl,
                        imageUrl = item.imageUrl, // imageUrl 추가
                        currentSummary = item.currentSummary ?: generateDummySummary(item.name, item.category)
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
    
    /**
     * 랜드마크 상세 정보 조회 (요약 포함)
     */
    suspend fun getLandmarkDetail(
        landmarkId: Long
    ): Result<POIData> = withContext(Dispatchers.IO) {
        try {
            val response = poiApi.getLandmarkDetail(landmarkId)
            if (response.success) {
                val landmarkData = response.data
                val poiData = POIData(
                    id = landmarkData.id,
                    name = landmarkData.name,
                    category = landmarkData.category,
                    address = "", // 백엔드에서 제공하지 않음
                    latitude = landmarkData.latitude,
                    longitude = landmarkData.longitude,
                    phone = null,
                    rating = null,
                    distance = null,
                    isOpen = null,
                    openHours = null,
                    thumbnailUrl = landmarkData.thumbnailUrl,
                    imageUrl = landmarkData.imageUrl, // imageUrl 추가
                    currentSummary = landmarkData.currentSummary ?: generateDummySummary(landmarkData.name, landmarkData.category)
                )
                Result.success(poiData)
            } else {
                Result.failure(Exception("Failed to get landmark detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * currentSummary가 없을 때 카테고리와 이름을 기반으로 더미 요약 생성
     */
    private fun generateDummySummary(name: String, category: String): String {
        return when(category.uppercase()) {
            "LIBRARY" -> "$name 도서관입니다. 학습과 연구를 위한 다양한 자료와 편안한 공간을 제공합니다."
            "RESTAURANT" -> "$name 식당입니다. 맛있고 정성스러운 음식을 제공하는 곳입니다."
            "CAFE" -> "$name 카페입니다. 향긋한 커피와 아늑한 분위기를 즐길 수 있는 공간입니다."
            "DORMITORY" -> "$name 기숙사입니다. 학생들의 편안한 생활과 학습을 지원하는 주거공간입니다."
            "FOOD_TRUCK" -> "$name 푸드트럭입니다. 간편하고 맛있는 음식을 제공하는 이동식 매장입니다."
            "EVENT" -> "$name 행사장입니다. 다양한 이벤트와 활동이 진행되는 공간입니다."
            "UNIVERSITY_BUILDING" -> "$name 대학건물입니다. 학습과 연구활동이 이루어지는 교육시설입니다."
            else -> "$name 입니다. 이 장소에 대한 자세한 정보를 준비 중입니다."
        }
    }
}