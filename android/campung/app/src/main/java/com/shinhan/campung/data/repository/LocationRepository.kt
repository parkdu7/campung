package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.remote.api.LocationApi
import com.shinhan.campung.data.remote.request.LocationShareRespondRequest
import com.shinhan.campung.data.remote.response.LocationShareRespondResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationApi: LocationApi,
    private val authDataStore: AuthDataStore
) {

    // 토큰 가져오기 헬퍼 메서드
    private suspend fun getAuthToken(): String {
        val token = authDataStore.userIdFlow.first()
            ?: throw IllegalStateException("인증 토큰이 없습니다. 다시 로그인해주세요.")
        return "Bearer $token"
    }

    // 위치 공유 요청 수락
    suspend fun acceptLocationShareRequest(locationRequestId: Long): LocationShareRespondResponse {
        val request = LocationShareRespondRequest(
            locationRequestId = locationRequestId,
            action = "accept"
        )
        return locationApi.respondToLocationShareRequest(request)
    }

    // 위치 공유 요청 거절
    suspend fun rejectLocationShareRequest(locationRequestId: Long): LocationShareRespondResponse {
        val request = LocationShareRespondRequest(
            locationRequestId = locationRequestId,
            action = "reject"
        )
        return locationApi.respondToLocationShareRequest(request)
    }

    // 위치 공유 요청 보내기
    suspend fun sendLocationShareRequest(targetUserId: String): LocationShareRespondResponse {
        val request = mapOf("targetUserId" to targetUserId)
        return locationApi.sendLocationShareRequest(request)
    }

    // 현재 위치 공유 상태 조회
    suspend fun getLocationShareStatus(): Map<String, Any> {
        return locationApi.getLocationShareStatus()
    }

    // 위치 공유 중단
    suspend fun stopLocationShare(shareId: Long) {
        locationApi.stopLocationShare(shareId)
    }

    // 공유 중인 위치 목록 조회
    suspend fun getSharedLocations(): List<Map<String, Any>> {
        return locationApi.getSharedLocations()
    }
}