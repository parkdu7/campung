package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.request.FriendRequest
import com.shinhan.campung.data.remote.response.FriendResponse
import retrofit2.http.*

interface FriendApi {

    // 친구 요청 보내기
    @POST("api/friends/requests")
    suspend fun sendFriendRequest(
        @Header("Authorization") authorization: String,
        @Body request: FriendRequest
    ): FriendResponse

    // 친구 요청 수락
    @PUT("api/friends/requests/{friendshipId}/accept")
    suspend fun acceptFriendRequest(
        @Header("Authorization") authorization: String,
        @Path("friendshipId") friendshipId: Long
    ): FriendResponse

    // 친구 요청 거절
    @PUT("api/friends/requests/{friendshipId}/reject")
    suspend fun rejectFriendRequest(
        @Header("Authorization") authorization: String,
        @Path("friendshipId") friendshipId: Long
    ): String

    // 받은 친구 요청 목록 조회
    @GET("api/friends/requests/received")
    suspend fun getReceivedFriendRequests(
        @Header("Authorization") authorization: String
    ): List<FriendResponse>

    // 보낸 친구 요청 목록 조회
    @GET("api/friends/requests/sent")
    suspend fun getSentFriendRequests(
        @Header("Authorization") authorization: String
    ): List<FriendResponse>

    // 친구 목록 조회
    @GET("api/friends")
    suspend fun getFriendsList(
        @Header("Authorization") authorization: String
    ): List<FriendResponse>

    // 친구 끊기
    @DELETE("api/friends/{friendshipId}")
    suspend fun removeFriend(
        @Header("Authorization") authorization: String,
        @Path("friendshipId") friendshipId: Long
    ): String
}