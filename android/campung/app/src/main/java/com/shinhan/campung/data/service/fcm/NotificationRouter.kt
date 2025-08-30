package com.shinhan.campung.data.service.fcm

import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import javax.inject.Inject

/**
 * FCM 알림을 적절한 Handler로 라우팅하는 Router
 * 각 Handler의 canHandle() 메서드를 확인하여 적절한 Handler를 선택
 */
class NotificationRouter @Inject constructor(
    private val handlers: Set<@JvmSuppressWildcards NotificationHandler>
) {
    companion object {
        private const val TAG = "NotificationRouter"
    }
    
    /**
     * FCM 메시지를 적절한 Handler로 라우팅
     * @param data FCM 메시지의 data 필드
     * @param notification FCM 메시지의 notification 필드
     */
    fun route(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val type = data["type"]
        val contentId = data["contentId"]
        
        Log.d(TAG, "=== 알림 라우팅 시작 ===")
        Log.d(TAG, "데이터 - type: $type, contentId: $contentId")
        Log.d(TAG, "전체 데이터: $data")
        Log.d(TAG, "사용 가능한 handlers: ${handlers.size}개")
        handlers.forEach { handler ->
            Log.d(TAG, "Handler: ${handler.javaClass.simpleName}")
        }
        
        // 타입별로 Handler 우선순위 정렬 (GeneralNotificationHandler는 마지막)
        val sortedHandlers = handlers.sortedWith { h1, h2 ->
            when {
                h1.javaClass.simpleName.contains("General") -> 1
                h2.javaClass.simpleName.contains("General") -> -1
                else -> 0
            }
        }
        
        Log.d(TAG, "정렬된 handlers:")
        sortedHandlers.forEach { handler ->
            Log.d(TAG, "  - ${handler.javaClass.simpleName}, canHandle($type): ${handler.canHandle(type)}")
        }
        
        // 첫 번째로 처리 가능한 Handler에게 위임
        val selectedHandler = sortedHandlers.firstOrNull { it.canHandle(type) }
        
        if (selectedHandler != null) {
            Log.d(TAG, "Handler 선택: ${selectedHandler.javaClass.simpleName}")
            selectedHandler.handle(data, notification)
        } else {
            Log.w(TAG, "처리 가능한 Handler가 없음 - type: $type")
        }
        Log.d(TAG, "=== 알림 라우팅 완료 ===")
    }
}