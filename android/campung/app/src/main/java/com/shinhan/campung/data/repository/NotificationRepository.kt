package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.remote.api.NotificationApi
import com.shinhan.campung.data.remote.response.NotificationResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val authDataStore: AuthDataStore
) {

    // 토큰 가져오기 헬퍼 메서드
    private suspend fun getAuthToken(): String {
        val token = authDataStore.userIdFlow.first()
            ?: throw IllegalStateException("인증 토큰이 없습니다. 다시 로그인해주세요.")
        return "Bearer $token"
    }

    // 알림 목록 조회
    suspend fun getNotifications(): List<NotificationResponse> {
        val response = notificationApi.getNotifications()
        return if (response.success && response.data != null) {
            response.data
        } else {
            emptyList()
        }
    }

    // 읽지 않은 알림 개수 조회
    suspend fun getUnreadCount(): Int {
        return notificationApi.getUnreadCount()
    }

    // 알림 읽음 처리
    suspend fun markAsRead(notificationId: Long) {
        notificationApi.markAsRead(notificationId)
    }

    // 알림 삭제
    suspend fun deleteNotification(notificationId: Long) {
        notificationApi.deleteNotification(notificationId)
    }

    // 모든 알림 읽음 처리
    suspend fun markAllAsRead() {
        notificationApi.markAllAsRead()
    }

    // 특정 타입의 알림만 조회
    suspend fun getNotificationsByType(type: String): List<NotificationResponse> {
        return notificationApi.getNotificationsByType(type)
    }

    // 알림 설정 조회
    suspend fun getNotificationSettings(): Map<String, Boolean> {
        return notificationApi.getNotificationSettings()
    }

    // 알림 설정 업데이트
    suspend fun updateNotificationSettings(settings: Map<String, Boolean>) {
        notificationApi.updateNotificationSettings(settings)
    }

    // FCM 토큰 등록
    suspend fun registerFcmToken(token: String) {
        val request = mapOf("fcmToken" to token)
        notificationApi.registerFcmToken(request)
    }
}