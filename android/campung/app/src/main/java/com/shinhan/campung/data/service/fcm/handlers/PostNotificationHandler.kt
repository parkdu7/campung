package com.shinhan.campung.data.service.fcm.handlers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.shinhan.campung.MainActivity
import com.shinhan.campung.R
import com.shinhan.campung.data.service.fcm.NotificationHandler
import org.json.JSONObject
import javax.inject.Inject

/**
 * 게시글 관련 알림 (좋아요, 댓글) 처리 Handler
 */
class PostNotificationHandler @Inject constructor(
    private val context: Context
) : NotificationHandler {
    
    companion object {
        private const val TAG = "PostNotificationHandler"
        const val GENERAL_CHANNEL_ID = "general_channel"
    }
    
    override fun canHandle(type: String?): Boolean {
        return type in listOf("post_like", "post_comment", "normal")
    }
    
    override fun handle(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        Log.d(TAG, "=== 게시글 알림 처리 시작 ===")
        Log.d(TAG, "원본 data: $data")
        
        // 1차: 직접 contentId 추출 시도
        var contentId = data["contentId"]?.toLongOrNull()
        Log.d(TAG, "1차 contentId 추출: $contentId")
        
        // 2차: contentId가 없으면 data 필드에서 JSON 파싱 시도
        if (contentId == null) {
            val nestedData = data["data"]
            Log.d(TAG, "중첩 data 필드: $nestedData")
            
            if (nestedData != null) {
                try {
                    val jsonObject = JSONObject(nestedData)
                    contentId = jsonObject.optLong("contentId", 0L).takeIf { it > 0L }
                    Log.d(TAG, "JSON 파싱으로 추출된 contentId: $contentId")
                } catch (e: Exception) {
                    Log.e(TAG, "JSON 파싱 실패: $nestedData", e)
                }
            }
        }
        
        val userName = data["userName"] ?: "알 수 없는 사용자"
        val type = data["type"] ?: ""
        
        Log.d(TAG, "최종 데이터 - contentId: $contentId, userName: $userName, type: $type")
        
        val title = notification?.title ?: when (type) {
            "post_like" -> "좋아요 알림"
            "post_comment" -> "댓글 알림"
            else -> "게시글 알림"
        }
        
        val body = notification?.body ?: when (type) {
            "post_like" -> "$userName 님이 회원님의 게시글을 좋아합니다"
            "post_comment" -> "$userName 님이 회원님의 게시글에 댓글을 남겼습니다"
            else -> "$userName 님이 회원님의 게시글에 반응했습니다"
        }
        
        showNotification(title, body, contentId, type)
    }
    
    private fun showNotification(title: String, body: String, contentId: Long?, notificationType: String) {
        Log.d(TAG, "=== 게시글 알림 생성 시작 ===")
        Log.d(TAG, "제목: $title, 내용: $body")
        Log.d(TAG, "contentId: $contentId, 타입: $notificationType")
        
        // FCM 데이터를 Intent에 명시적으로 포함
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // contentId와 type을 Intent extras에 추가
            contentId?.let { 
                putExtra("contentId", it)
                Log.d(TAG, "Intent에 contentId 추가: $it")
            }
            putExtra("type", notificationType)
            putExtra("fcm_notification", true) // FCM 알림임을 표시
            Log.d(TAG, "Intent extras 설정 완료 - type: $notificationType")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        Log.d(TAG, "게시글 알림 표시 완료 - contentId: $contentId")
    }
}