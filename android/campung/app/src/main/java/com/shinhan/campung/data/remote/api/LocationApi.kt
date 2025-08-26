package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.request.LocationShareRespondRequest
import com.shinhan.campung.data.remote.response.LocationShareRespondResponse
import retrofit2.http.*

interface LocationApi {

    // 위치 공유 요청에 응답 (수락/거절)
    @POST("location/share/respond")
    suspend fun respondToLocationShareRequest(@Body request: LocationShareRespondRequest): LocationShareRespondResponse

    // 위치 공유 요청 보내기
    @POST("location/share/request")
    suspend fun sendLocationShareRequest(@Body request: Map<String, String>): LocationShareRespondResponse

    // 현재 위치 공유 상태 조회
    @GET("location/share/status")
    suspend fun getLocationShareStatus(): Map<String, Any>

    // 위치 공유 중단
    @DELETE("location/share/{shareId}")
    suspend fun stopLocationShare(@Path("shareId") shareId: Long)

    // 공유 중인 위치 목록 조회
    @GET("location/share/list")
    suspend fun getSharedLocations(): List<Map<String, Any>>

    // 받은 위치 공유 요청 목록 조회
    @GET("location/share/requests/received")
    suspend fun getReceivedLocationShareRequests(): List<Map<String, Any>>

    // 보낸 위치 공유 요청 목록 조회
    @GET("location/share/requests/sent")
    suspend fun getSentLocationShareRequests(): List<Map<String, Any>>

    // 위치 업데이트 (실시간 위치 공유)
    @POST("location/update")
    suspend fun updateLocation(@Body request: Map<String, Any>)

    // 친구의 현재 위치 조회
    @GET("location/friend/{friendId}")
    suspend fun getFriendLocation(@Path("friendId") friendId: String): Map<String, Any>
}