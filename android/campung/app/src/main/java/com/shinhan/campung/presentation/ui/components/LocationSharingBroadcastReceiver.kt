package com.shinhan.campung.presentation.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.shinhan.campung.data.model.SharedLocation
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 위치 공유 브로드캐스트를 수신하는 모듈화된 컴포넌트
 */
@Composable
fun LocationSharingBroadcastReceiver(
    onLocationReceived: (SharedLocation) -> Unit = {},
    onLocationRemoved: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("LocationSharingBroadcast", "브로드캐스트 수신됨 - action: ${intent?.action}")
                
                when (intent?.action) {
                    "com.shinhan.campung.LOCATION_SHARED" -> {
                        handleLocationShared(intent, onLocationReceived)
                    }
                    "com.shinhan.campung.LOCATION_SHARING_STOPPED" -> {
                        handleLocationSharingStopped(intent, onLocationRemoved)
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("com.shinhan.campung.LOCATION_SHARED")
            addAction("com.shinhan.campung.LOCATION_SHARING_STOPPED")
        }
        
        // API 레벨에 따른 브로드캐스트 수신기 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        Log.d("LocationSharingBroadcast", "브로드캐스트 리시버 등록됨")
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
                Log.d("LocationSharingBroadcast", "브로드캐스트 리시버 해제됨")
            } catch (e: IllegalArgumentException) {
                Log.w("LocationSharingBroadcast", "브로드캐스트 리시버 해제 실패 (이미 해제됨)", e)
            }
        }
    }
}

/**
 * 위치 공유 브로드캐스트 처리
 */
private fun handleLocationShared(intent: Intent, onLocationReceived: (SharedLocation) -> Unit) {
    Log.d("LocationSharingBroadcast", "위치 공유 브로드캐스트 처리 시작")
    
    try {
        val userName = intent.getStringExtra("userName")
        val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull()
        val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull()
        val shareId = intent.getStringExtra("shareId")
        val displayUntil = intent.getStringExtra("displayUntil") // 기존 코드 호환
        
        Log.d("LocationSharingBroadcast", 
            "수신된 데이터 - userName: $userName, lat: $latitude, lng: $longitude, shareId: $shareId, displayUntil: $displayUntil")
        
        if (userName != null && latitude != null && longitude != null && shareId != null && displayUntil != null) {
            try {
                // displayUntil 문자열을 LocalDateTime으로 파싱
                val displayUntilDateTime = LocalDateTime.parse(displayUntil, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                
                val sharedLocation = SharedLocation(
                    shareId = shareId,
                    userName = userName,
                    latitude = latitude,
                    longitude = longitude,
                    displayUntil = displayUntilDateTime
                )
                
                onLocationReceived(sharedLocation)
                Log.d("LocationSharingBroadcast", "위치 공유 데이터 처리 완료: ${userName}님")
            } catch (e: Exception) {
                Log.e("LocationSharingBroadcast", "displayUntil 파싱 실패: $displayUntil", e)
                // displayUntil 파싱 실패시 기본값 사용
                val sharedLocation = SharedLocation(
                    shareId = shareId,
                    userName = userName,
                    latitude = latitude,
                    longitude = longitude,
                    displayUntil = LocalDateTime.now().plusHours(1) // 1시간 후 기본값
                )
                onLocationReceived(sharedLocation)
            }
        } else {
            Log.w("LocationSharingBroadcast", "불완전한 위치 공유 데이터 수신됨")
        }
    } catch (e: Exception) {
        Log.e("LocationSharingBroadcast", "위치 공유 브로드캐스트 처리 실패", e)
    }
}

/**
 * 위치 공유 중단 브로드캐스트 처리
 */
private fun handleLocationSharingStopped(intent: Intent, onLocationRemoved: (String) -> Unit) {
    Log.d("LocationSharingBroadcast", "위치 공유 중단 브로드캐스트 처리 시작")
    
    try {
        val shareId = intent.getStringExtra("shareId")
        val userName = intent.getStringExtra("userName")
        
        Log.d("LocationSharingBroadcast", "위치 공유 중단 - shareId: $shareId, userName: $userName")
        
        if (shareId != null) {
            onLocationRemoved(shareId)
            Log.d("LocationSharingBroadcast", "위치 공유 중단 처리 완료: $userName")
        } else {
            Log.w("LocationSharingBroadcast", "shareId가 없는 위치 공유 중단 브로드캐스트")
        }
    } catch (e: Exception) {
        Log.e("LocationSharingBroadcast", "위치 공유 중단 브로드캐스트 처리 실패", e)
    }
}

/**
 * 통합 위치공유 브로드캐스트 수신 컴포넌트
 * 기존 LocationSharingManager와 호환
 */
@Composable
fun IntegratedLocationSharingBroadcastReceiver(
    locationSharingManager: com.shinhan.campung.data.service.LocationSharingManager? = null
) {
    val context = LocalContext.current
    
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("IntegratedLocationBroadcast", "브로드캐스트 수신됨 - action: ${intent?.action}")
                
                if (intent?.action == "com.shinhan.campung.LOCATION_SHARED") {
                    try {
                        val userName = intent.getStringExtra("userName")
                        val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull()
                        val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull()
                        val displayUntil = intent.getStringExtra("displayUntil")
                        val shareId = intent.getStringExtra("shareId")
                        
                        Log.d("IntegratedLocationBroadcast", 
                            "브로드캐스트 데이터: userName=$userName, lat=$latitude, lng=$longitude, displayUntil=$displayUntil, shareId=$shareId")
                        
                        if (userName != null && latitude != null && longitude != null && displayUntil != null && shareId != null) {
                            // 기존 LocationSharingManager 사용
                            locationSharingManager?.addSharedLocation(
                                userName, latitude, longitude, displayUntil, shareId
                            )
                            Log.d("IntegratedLocationBroadcast", "LocationSharingManager.addSharedLocation 호출 완료")
                        } else {
                            Log.e("IntegratedLocationBroadcast", "브로드캐스트 데이터 누락 - 처리 중단")
                        }
                    } catch (e: Exception) {
                        Log.e("IntegratedLocationBroadcast", "브로드캐스트 처리 실패", e)
                    }
                }
            }
        }
        
        val filter = IntentFilter("com.shinhan.campung.LOCATION_SHARED")
        
        // API 레벨에 따른 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        // LocalBroadcastManager도 등록
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .registerReceiver(receiver, filter)
            Log.d("IntegratedLocationBroadcast", "LocalBroadcast 수신기도 등록 완료")
        } catch (e: Exception) {
            Log.e("IntegratedLocationBroadcast", "LocalBroadcast 수신기 등록 실패", e)
        }
        
        Log.d("IntegratedLocationBroadcast", "브로드캐스트 리시버 등록 완료")
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
                Log.d("IntegratedLocationBroadcast", "전역 브로드캐스트 수신기 해제 완료")
            } catch (e: IllegalArgumentException) {
                Log.w("IntegratedLocationBroadcast", "전역 브로드캐스트 수신기 해제 실패", e)
            }
            
            try {
                androidx.localbroadcastmanager.content.LocalBroadcastManager
                    .getInstance(context)
                    .unregisterReceiver(receiver)
                Log.d("IntegratedLocationBroadcast", "LocalBroadcast 수신기 해제 완료")
            } catch (e: Exception) {
                Log.w("IntegratedLocationBroadcast", "LocalBroadcast 수신기 해제 실패", e)
            }
        }
    }
}