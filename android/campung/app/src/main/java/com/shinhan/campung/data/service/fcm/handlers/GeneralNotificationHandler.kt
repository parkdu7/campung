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
 * 일반 알림 처리 Handler (기본 핸들러)
 */
class GeneralNotificationHandler @Inject constructor(
    private val context: Context
) : NotificationHandler {
    
    companion object {
        private const val TAG = "GeneralNotificationHandler"
        const val GENERAL_CHANNEL_ID = "general_channel"
    }
    
    override fun canHandle(type: String?): Boolean {
        // 다른 Handler에서 처리하지 않는 모든 타입을 처리 (fallback)
        return true
    }
    
    override fun handle(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        Log.d(TAG, "=== 일반 알림 처리 시작 ===")
        Log.d(TAG, "원본 data: $data")
        
        val title = notification?.title ?: "캠핑 알림"
        val body = notification?.body ?: "새로운 알림이 도착했습니다"
        
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
        
        val type = data["type"]
        
        Log.d(TAG, "최종 데이터 - contentId: $contentId, type: $type")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // contentId가 있으면 Intent에 추가
            contentId?.let { 
                putExtra("contentId", it)
                Log.d(TAG, "GeneralHandler에서 contentId 추가: $it")
            }
            type?.let { putExtra("type", it) }
        }
        
        showNotification(title, body, intent)
    }
    
    private fun showNotification(title: String, body: String, intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        
        Log.d(TAG, "일반 알림 표시 완료")
    }
}