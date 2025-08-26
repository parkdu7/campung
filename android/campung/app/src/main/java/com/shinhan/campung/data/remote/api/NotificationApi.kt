package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.response.NotificationResponse
import com.shinhan.campung.data.remote.response.NotificationListResponse
import retrofit2.http.*

interface NotificationApi {

    // 알림 목록 조회
    @GET("notifications")
    suspend fun getNotifications(): NotificationListResponse

    // 읽지 않은 알림 개수 조회
    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Int

    // 알림 읽음 처리
    @PUT("notifications/{notificationId}/read")
    suspend fun markAsRead(@Path("notificationId") notificationId: Long)

    // 알림 삭제
    @DELETE("notifications/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: Long)

    // 모든 알림 읽음 처리
    @PUT("notifications/read-all")
    suspend fun markAllAsRead()

    // 특정 타입의 알림 조회
    @GET("notifications")
    suspend fun getNotificationsByType(@Query("type") type: String): List<NotificationResponse>

    // 알림 설정 조회
    @GET("notifications/settings")
    suspend fun getNotificationSettings(): Map<String, Boolean>

    // 알림 설정 업데이트
    @PUT("notifications/settings")
    suspend fun updateNotificationSettings(@Body settings: Map<String, Boolean>)

    // FCM 토큰 등록/업데이트
    @POST("notifications/fcm-token")
    suspend fun registerFcmToken(@Body request: Map<String, String>)

    // 테스트 알림 전송 (개발용)
    @POST("notifications/test")
    suspend fun sendTestNotification(@Body request: Map<String, String>)
}