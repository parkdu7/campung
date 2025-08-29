package com.shinhan.campung.data.service.fcm.handlers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.RemoteMessage
import com.shinhan.campung.MainActivity
import com.shinhan.campung.R
import com.shinhan.campung.data.service.LocationShareActionReceiver
import com.shinhan.campung.data.service.fcm.NotificationHandler
import javax.inject.Inject

/**
 * 위치 공유 관련 알림 처리 Handler
 */
class LocationShareNotificationHandler @Inject constructor(
    private val context: Context
) : NotificationHandler {
    
    companion object {
        private const val TAG = "LocationShareHandler"
        const val LOCATION_SHARE_CHANNEL_ID = "location_share_channel"
        const val GENERAL_CHANNEL_ID = "general_channel"
    }
    
    override fun canHandle(type: String?): Boolean {
        return type in listOf("location_share_request", "location_share", "location_share_rejected")
    }
    
    override fun handle(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        when (data["type"]) {
            "location_share_request" -> handleLocationShareRequest(data, notification)
            "location_share" -> handleLocationShared(data, notification)
            "location_share_rejected" -> handleLocationShareRejected(data, notification)
        }
    }
    
    private fun handleLocationShareRequest(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val fromUserName = data["fromUserName"] ?: "알 수 없는 사용자"
        val message = data["message"] ?: ""
        val shareRequestId = data["shareRequestId"]?.toLongOrNull()
        val hasActionButtons = data["action_buttons"] == "true"
        
        Log.d(TAG, "위치 공유 요청 - fromUserName: $fromUserName, shareRequestId: $shareRequestId")
        
        val title = notification?.title ?: "위치 공유 요청"
        val body = notification?.body ?: "$fromUserName 님이 위치를 요청했습니다: $message"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", "location_share_request")
            putExtra("shareRequestId", shareRequestId)
            putExtra("fromUserName", fromUserName)
            putExtra("message", message)
        }
        
        if (hasActionButtons) {
            showNotificationWithActions(title, body, intent, shareRequestId ?: 0L, LOCATION_SHARE_CHANNEL_ID)
        } else {
            showSimpleNotification(title, body, intent, LOCATION_SHARE_CHANNEL_ID)
        }
    }
    
    private fun handleLocationShared(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val userName = data["userName"] ?: "알 수 없는 사용자"
        val latitude = data["latitude"]
        val longitude = data["longitude"]
        val displayUntil = data["displayUntil"] ?: ""
        val shareId = data["shareId"] ?: ""
        
        Log.d(TAG, "위치 공유 데이터 수신 - userName: $userName, lat: $latitude, lng: $longitude")
        
        val title = notification?.title ?: "위치가 공유되었습니다"
        val body = notification?.body ?: "$userName 님이 위치를 공유했습니다"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", "location_share")
            putExtra("userName", userName)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("displayUntil", displayUntil)
            putExtra("shareId", shareId)
        }
        
        // 위치 공유 데이터를 전역적으로 브로드캐스트
        val broadcastIntent = Intent("com.shinhan.campung.LOCATION_SHARED").apply {
            putExtra("userName", userName)
            putExtra("latitude", latitude)
            putExtra("longitude", longitude)
            putExtra("displayUntil", displayUntil)
            putExtra("shareId", shareId)
        }
        
        context.sendBroadcast(broadcastIntent)
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
        
        showSimpleNotification(title, body, intent, GENERAL_CHANNEL_ID)
    }
    
    private fun handleLocationShareRejected(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val userName = data["userName"] ?: "알 수 없는 사용자"
        
        Log.d(TAG, "위치 공유 거절 - userName: $userName")
        
        val title = notification?.title ?: "위치 공유가 거절되었습니다"
        val body = notification?.body ?: "$userName 님이 위치 공유를 거절했습니다"
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", "location_share_rejected")
            putExtra("userName", userName)
        }
        
        showSimpleNotification(title, body, intent, GENERAL_CHANNEL_ID)
    }
    
    private fun showNotificationWithActions(
        title: String,
        body: String,
        intent: Intent,
        requestId: Long,
        channelId: String
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 수락 액션
        val acceptIntent = Intent(context, LocationShareActionReceiver::class.java).apply {
            action = "ACCEPT_LOCATION_SHARE"
            putExtra("shareRequestId", requestId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            (requestId * 10 + 1).toInt(),
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 거절 액션
        val rejectIntent = Intent(context, LocationShareActionReceiver::class.java).apply {
            action = "REJECT_LOCATION_SHARE"
            putExtra("shareRequestId", requestId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            (requestId * 10 + 2).toInt(),
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "수락", acceptPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "거절", rejectPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestId.toInt(), notification)
    }
    
    private fun showSimpleNotification(title: String, body: String, intent: Intent, channelId: String) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
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
    }
}