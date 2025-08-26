package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.request.FriendRequest
import com.shinhan.campung.data.remote.response.FriendResponse
import retrofit2.http.*

interface FriendApi {

    // 친구 요청 보내기
    @POST("friends/requests")
    suspend fun sendFriendRequest(
        @Body request: FriendRequest
    ): FriendResponse

    // 친구 요청 수락
    @PUT("friends/requests/{friendshipId}/accept")
    suspend fun acceptFriendRequest(
        @Path("friendshipId") friendshipId: Long
    ): FriendResponse

    // 친구 요청 거절
    @PUT("friends/requests/{friendshipId}/reject")
    suspend fun rejectFriendRequest(
        @Path("friendshipId") friendshipId: Long
    ): String

    // 받은 친구 요청 목록 조회
    @GET("friends/requests/received")
    suspend fun getReceivedFriendRequests(): List<FriendResponse>

    // 보낸 친구 요청 목록 조회
    @GET("friends/requests/sent")
    suspend fun getSentFriendRequests(): List<FriendResponse>

    // 친구 목록 조회
    @GET("friends")
    suspend fun getFriendsList(): List<FriendResponse>

    // 친구 끊기
    @DELETE("api/friends/{friendshipId}")
    suspend fun removeFriend(
        @Path("friendshipId") friendshipId: Long
    ): String
}