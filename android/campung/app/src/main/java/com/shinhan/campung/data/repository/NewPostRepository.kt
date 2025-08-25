package com.shinhan.campung.data.repository

import android.util.Log
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.location.LocationTracker
import com.shinhan.campung.data.remote.dto.NewPostEvent
import com.shinhan.campung.data.websocket.WebSocketService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPostRepository @Inject constructor(
    private val webSocketService: WebSocketService,
    private val locationTracker: LocationTracker,
    private val authDataStore: AuthDataStore
) {
    
    val connectionState: StateFlow<Boolean> = webSocketService.connectionState
    val newPostEvent: StateFlow<NewPostEvent?> = webSocketService.newPostEvent
    val currentGeohash: StateFlow<String?> = locationTracker.currentGeohash
    
    suspend fun startService() {
        // 사용자 ID 가져오기
        val userId = authDataStore.userIdFlow.first()
        Log.d(TAG, "Retrieved userId from DataStore: $userId")
        
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "UserId is null or blank, cannot start WebSocket service")
            return
        }
        
        // 위치 추적 시작
        locationTracker.startTracking()
        
        // 웹소켓 연결
        webSocketService.connect(userId)
    }
    
    fun stopService() {
        locationTracker.stopTracking()
        webSocketService.disconnect()
    }
    
    fun subscribeToLocation(geohash: String) {
        webSocketService.subscribe(geohash)
    }
    
    fun clearNewPostEvent() {
        // 이벤트 처리 후 클리어하는 용도
        // StateFlow를 직접 조작할 수 없으므로 WebSocketService에 메서드 추가 필요
    }
    
    companion object {
        private const val TAG = "NewPostRepository"
    }
}