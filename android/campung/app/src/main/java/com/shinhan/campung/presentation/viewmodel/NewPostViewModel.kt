package com.shinhan.campung.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.remote.dto.NewPostEvent
import com.shinhan.campung.data.repository.NewPostRepository
import com.shinhan.campung.util.GeohashUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: NewPostRepository
) : ViewModel() {
    
    val isConnected = mutableStateOf(false)
    val showNotification = mutableStateOf(false)
    val latestPost = mutableStateOf<NewPostEvent?>(null)
    
    private var currentGeohash: String? = null
    
    init {
        observeStates()
    }
    
    fun startNewPostService() {
        Log.d(TAG, "Starting new post service")
        viewModelScope.launch {
            repository.startService()
        }
    }
    
    fun stopNewPostService() {
        repository.stopService()
    }
    
    fun dismissNotification() {
        showNotification.value = false
    }
    
    private fun observeStates() {
        viewModelScope.launch {
            // 연결 상태 관찰
            repository.connectionState.collect { connected ->
                Log.d(TAG, "Connection state changed: $connected")
                isConnected.value = connected
                // 연결되면 대기 중인 Geohash로 구독
                if (connected && currentGeohash != null) {
                    Log.d(TAG, "Auto-subscribing to geohash: $currentGeohash")
                    repository.subscribeToLocation(currentGeohash!!)
                }
            }
        }
        
        viewModelScope.launch {
            // Geohash 변경 관찰 (재구독)
            repository.currentGeohash.collect { geohash ->
                Log.d(TAG, "Geohash changed: $currentGeohash -> $geohash")
                if (geohash != null && geohash != currentGeohash) {
                    currentGeohash = geohash
                    Log.i(TAG, "🎯 Current Geohash for testing: $geohash")
                    Log.i(TAG, "📍 Use this Geohash to test new post notifications!")
                    
                    // 테스트 좌표의 Geohash도 계산해보기
                    val testGeohash = GeohashUtil.encode(36.1072124, 128.4164663)
                    Log.i(TAG, "🧪 Test coordinates (36.1072124, 128.4164663) = $testGeohash")
                    Log.i(TAG, "🤔 Geohash match: ${geohash == testGeohash}")
                    
                    // WebSocket이 연결된 후에만 구독
                    if (isConnected.value) {
                        Log.d(TAG, "Subscribing to geohash: $geohash")
                        repository.subscribeToLocation(geohash)
                    } else {
                        Log.w(TAG, "WebSocket not connected, waiting to subscribe to: $geohash")
                    }
                }
            }
        }
        
        viewModelScope.launch {
            // 새 게시글 이벤트 관찰
            repository.newPostEvent.collect { event ->
                Log.d(TAG, "New post event received: $event")
                if (event != null) {
                    latestPost.value = event
                    showNotification.value = true
                    Log.d(TAG, "Showing notification for new post: ${event.postId}")
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopNewPostService()
    }
    
    companion object {
        private const val TAG = "NewPostViewModel"
    }
}