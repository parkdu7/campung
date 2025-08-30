package com.shinhan.campung.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.shinhan.campung.data.remote.dto.NewPostEvent
import com.shinhan.campung.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketService @Inject constructor(
    private val gson: Gson
) {
    private var webSocketClient: WebSocketClient? = null
    private var currentSubscription: String? = null
    private var currentUserId: String? = null  // 현재 사용자 ID 저장
    
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState
    
    private val _newPostEvent = MutableStateFlow<NewPostEvent?>(null)
    val newPostEvent: StateFlow<NewPostEvent?> = _newPostEvent
    
    fun connect(userId: String) {
        disconnect()
        
        // 현재 사용자 ID 저장
        currentUserId = userId
        
        try {
            // WebSocket URL 생성 (여러 패턴 시도)
            val wsUrl = "ws://campung.my:8080/ws" // 개발 포트로 시도
            val uri = URI(wsUrl)
            Log.d(TAG, "Connecting to WebSocket: $wsUrl")
            
            // WebSocket 헤더 추가
            val headers = mutableMapOf<String, String>()
            
            webSocketClient = object : WebSocketClient(uri, headers) {
                override fun onOpen(handshake: ServerHandshake?) {
                    Log.d(TAG, "WebSocket Connected")
                    
                    // STOMP CONNECT 메시지 전송 (UserChannelInterceptor에서 요구하는 userId 헤더)
                    val connectMessage = """CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000
userId:$userId

${'\u0000'}"""
                    Log.d(TAG, "Sending CONNECT message with userId: $userId")
                    Log.d(TAG, "Full CONNECT message: ${connectMessage.replace('\u0000', '\\')}")
                    send(connectMessage)
                }
                
                override fun onMessage(message: String?) {
                    Log.d(TAG, "Raw WebSocket message received: $message")
                    handleMessage(message)
                }
                
                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d(TAG, "WebSocket Closed: $reason")
                    _connectionState.value = false
                }
                
                override fun onError(ex: Exception?) {
                    Log.e(TAG, "WebSocket Error", ex)
                    _connectionState.value = false
                }
            }
            
            webSocketClient?.connect()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket", e)
        }
    }
    
    fun subscribe(geohash: String) {
        if (currentSubscription == geohash) return
        
        // WebSocket이 연결되어 있지 않으면 구독하지 않음
        if (!_connectionState.value) {
            Log.w(TAG, "WebSocket not connected, cannot subscribe to: $geohash")
            return
        }
        
        unsubscribe()
        
        val topic = "/topic/newpost/$geohash"
        val subscribeMessage = """SUBSCRIBE
destination:$topic
id:sub-$geohash

${'\u0000'}"""
        
        try {
            // 백엔드 정규식 패턴 체크: ^/topic/newpost/[0-9b-hj-km-np-z]{8}$
            if (!topic.matches(Regex("^/topic/newpost/[0-9b-hj-km-np-z]{8}$"))) {
                Log.e(TAG, "Invalid topic pattern: $topic")
                return
            }
            
            webSocketClient?.send(subscribeMessage)
            currentSubscription = geohash
            Log.d(TAG, "Subscribed to: $topic")
            Log.d(TAG, "Subscribe message: ${subscribeMessage.replace('\u0000', '\\')}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe", e)
        }
    }
    
    private fun unsubscribe() {
        currentSubscription?.let { geohash ->
            if (!_connectionState.value) return@let
            
            val unsubscribeMessage = """UNSUBSCRIBE
id:sub-$geohash

${'\u0000'}"""
            
            try {
                webSocketClient?.send(unsubscribeMessage)
                Log.d(TAG, "Unsubscribed from: /topic/newpost/$geohash")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsubscribe", e)
            }
        }
        currentSubscription = null
    }
    
    fun disconnect() {
        unsubscribe()
        webSocketClient?.close()
        webSocketClient = null
        currentUserId = null  // 사용자 ID 초기화
        _connectionState.value = false
    }
    
    private fun handleMessage(message: String?) {
        if (message == null) return
        
        try {
            Log.d(TAG, "Parsing message: ${message.take(100)}...") // 처음 100자만 로깅
            
            when {
                message.startsWith("CONNECTED") -> {
                    Log.d(TAG, "✅ STOMP Connected - Now accepting subscriptions")
                    _connectionState.value = true
                }
                message.startsWith("MESSAGE") -> {
                    Log.d(TAG, "📨 Received MESSAGE frame")
                    val bodyStartIndex = message.indexOf("\n\n") + 2
                    if (bodyStartIndex > 1) {
                        val body = message.substring(bodyStartIndex).trimEnd('\u0000')
                        Log.d(TAG, "Message body: $body")
                        val newPostEvent = gson.fromJson(body, NewPostEvent::class.java)
                        Log.d(TAG, "🔔 Parsed new post event: $newPostEvent")
                        
                        // 본인이 작성한 게시글인지 확인
                        val isOwnPost = newPostEvent.userId == currentUserId
                        Log.d(TAG, "Is own post? $isOwnPost (author: ${newPostEvent.userId}, current: $currentUserId)")
                        
                        // 본인 게시글이 아닐 때만 알림 이벤트 발생
                        if (!isOwnPost) {
                            _newPostEvent.value = newPostEvent
                            Log.d(TAG, "✅ Notification will be shown for other user's post")
                        } else {
                            Log.d(TAG, "🚫 Skipping notification for own post")
                        }
                    } else {
                        Log.w(TAG, "MESSAGE frame has no body")
                    }
                }
                message.startsWith("ERROR") -> {
                    Log.e(TAG, "❌ STOMP ERROR received: $message")
                }
                message.startsWith("RECEIPT") -> {
                    Log.d(TAG, "📋 STOMP RECEIPT: $message")
                }
                else -> {
                    Log.d(TAG, "🤔 Unknown STOMP frame type: ${message.take(20)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    companion object {
        private const val TAG = "WebSocketService"
    }
}