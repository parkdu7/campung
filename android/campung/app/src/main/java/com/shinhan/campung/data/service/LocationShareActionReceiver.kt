package com.shinhan.campung.data.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shinhan.campung.data.location.LocationTracker
import com.shinhan.campung.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationShareActionReceiver : BroadcastReceiver() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var locationTracker: LocationTracker

    companion object {
        private const val TAG = "LocationShareAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val shareRequestId = intent.getLongExtra("shareRequestId", 0L)
        
        Log.d(TAG, "액션 수신: $action, requestId: $shareRequestId")

        // 알림 제거
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(shareRequestId.toInt())

        when (action) {
            "ACCEPT_LOCATION_SHARE" -> {
                handleAccept(shareRequestId)
            }
            "REJECT_LOCATION_SHARE" -> {
                handleReject(shareRequestId)
            }
        }
    }

    private fun handleAccept(shareRequestId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 현재 위치 가져오기 (기존 LocationTracker의 함수 사용)
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    // 서버에 수락 응답 전송
                    authRepository.respondToLocationShareRequest(
                        shareRequestId = shareRequestId,
                        action = "accept",
                        latitude = location.latitude.toBigDecimal(),
                        longitude = location.longitude.toBigDecimal()
                    )
                    Log.d(TAG, "위치 공유 수락 완료")
                } else {
                    Log.e(TAG, "현재 위치를 가져올 수 없습니다")
                    // 위치 없이 거절 처리
                    authRepository.respondToLocationShareRequest(
                        shareRequestId = shareRequestId,
                        action = "reject",
                        latitude = null,
                        longitude = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "위치 공유 수락 실패", e)
            }
        }
    }

    private fun handleReject(shareRequestId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 서버에 거절 응답 전송
                authRepository.respondToLocationShareRequest(
                    shareRequestId = shareRequestId,
                    action = "reject",
                    latitude = null,
                    longitude = null
                )
                Log.d(TAG, "위치 공유 거절 완료")
            } catch (e: Exception) {
                Log.e(TAG, "위치 공유 거절 실패", e)
            }
        }
    }
}