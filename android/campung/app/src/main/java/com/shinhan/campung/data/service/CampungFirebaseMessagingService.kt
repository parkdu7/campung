package com.shinhan.campung.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shinhan.campung.MainActivity
import com.shinhan.campung.R
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CampungFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var authDataStore: AuthDataStore

    companion object {
        private const val TAG = "FCMService"
        const val LOCATION_SHARE_CHANNEL_ID = "location_share_channel"
        const val GENERAL_CHANNEL_ID = "general_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새 FCM 토큰: $token")
        
        // 서버에 토큰 업데이트
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepository.updateFcmToken(token)
                Log.d(TAG, "FCM 토큰 서버 업데이트 성공")
            } catch (e: Exception) {
                Log.e(TAG, "FCM 토큰 서버 업데이트 실패", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "FCM 메시지 수신: ${remoteMessage.from}")
        
        val data = remoteMessage.data
        val type = data["type"]
        
        Log.d(TAG, "메시지 타입: $type")
        Log.d(TAG, "메시지 데이터: $data")

        when (type) {
            "location_share_request" -> {
                handleLocationShareRequest(data, remoteMessage.notification)
            }
            "location_share" -> {
                handleLocationShared(data, remoteMessage.notification)
            }
            "location_share_rejected" -> {
                handleLocationShareRejected(data, remoteMessage.notification)
            }
            else -> {
                handleGeneralNotification(data, remoteMessage.notification)
            }
        }
    }

    private fun handleLocationShareRequest(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val fromUserName = data["fromUserName"] ?: "알 수 없는 사용자"
        val message = data["message"] ?: ""
        val shareRequestId = data["shareRequestId"]?.toLongOrNull()
        val hasActionButtons = data["action_buttons"] == "true"
        
        val title = notification?.title ?: "위치 공유 요청"
        val body = notification?.body ?: "$fromUserName 님이 위치를 요청했습니다: $message"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", "location_share_request")
            putExtra("shareRequestId", shareRequestId)
            putExtra("fromUserName", fromUserName)
            putExtra("message", message)
        }
        
        if (hasActionButtons) {
            showNotificationWithActions(
                title = title,
                body = body,
                intent = intent,
                requestId = shareRequestId ?: 0L,
                channelId = LOCATION_SHARE_CHANNEL_ID
            )
        } else {
            showNotification(title, body, intent, LOCATION_SHARE_CHANNEL_ID)
        }
    }

    private fun handleLocationShared(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val userName = data["userName"] ?: "알 수 없는 사용자"
        val latitude = data["latitude"]
        val longitude = data["longitude"]
        val displayUntil = data["displayUntil"] ?: ""
        val shareId = data["shareId"] ?: ""
        
        Log.d(TAG, "위치 공유 데이터 수신 - userName: $userName, lat: $latitude, lng: $longitude, displayUntil: $displayUntil")
        
        val title = notification?.title ?: "위치가 공유되었습니다"
        val body = notification?.body ?: "$userName 님이 위치를 공유했습니다"
        
        val intent = Intent(this, MainActivity::class.java).apply {
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
        Log.d(TAG, "브로드캐스트 전송 중 - action: com.shinhan.campung.LOCATION_SHARED")
        Log.d(TAG, "브로드캐스트 데이터 - userName: $userName, shareId: $shareId")
        
        // 전역 브로드캐스트와 LocalBroadcast 모두 전송
        sendBroadcast(broadcastIntent)
        
        // LocalBroadcastManager도 사용 (더 안전함)
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(this)
                .sendBroadcast(broadcastIntent)
            Log.d(TAG, "LocalBroadcast도 전송 완료")
        } catch (e: Exception) {
            Log.e(TAG, "LocalBroadcast 전송 실패", e)
        }
        
        Log.d(TAG, "브로드캐스트 전송 완료")
        
        showNotification(title, body, intent, GENERAL_CHANNEL_ID)
    }

    private fun handleLocationShareRejected(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val userName = data["userName"] ?: "알 수 없는 사용자"
        
        val title = notification?.title ?: "위치 공유가 거절되었습니다"
        val body = notification?.body ?: "$userName 님이 위치 공유를 거절했습니다"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", "location_share_rejected")
            putExtra("userName", userName)
        }
        
        showNotification(title, body, intent, GENERAL_CHANNEL_ID)
    }

    private fun handleGeneralNotification(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        val title = notification?.title ?: "캠핑 알림"
        val body = notification?.body ?: "새로운 알림이 도착했습니다"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        showNotification(title, body, intent, GENERAL_CHANNEL_ID)
    }

    private fun showNotificationWithActions(
        title: String,
        body: String,
        intent: Intent,
        requestId: Long,
        channelId: String
    ) {
        val pendingIntent = PendingIntent.getActivity(
            this, 
            requestId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 수락 액션
        val acceptIntent = Intent(this, LocationShareActionReceiver::class.java).apply {
            action = "ACCEPT_LOCATION_SHARE"
            putExtra("shareRequestId", requestId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            (requestId * 10 + 1).toInt(),
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 거절 액션
        val rejectIntent = Intent(this, LocationShareActionReceiver::class.java).apply {
            action = "REJECT_LOCATION_SHARE"
            putExtra("shareRequestId", requestId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this,
            (requestId * 10 + 2).toInt(),
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
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

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestId.toInt(), notification)
    }

    private fun showNotification(title: String, body: String, intent: Intent, channelId: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 위치 공유 채널
            val locationShareChannel = NotificationChannel(
                LOCATION_SHARE_CHANNEL_ID,
                "위치 공유",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "친구 위치 공유 요청 알림"
                enableVibration(true)
                enableLights(true)
            }

            // 일반 알림 채널
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "일반 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "일반적인 앱 알림"
            }

            notificationManager.createNotificationChannels(listOf(locationShareChannel, generalChannel))
        }
    }
}