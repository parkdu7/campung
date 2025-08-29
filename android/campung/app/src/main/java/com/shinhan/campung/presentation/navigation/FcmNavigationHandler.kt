package com.shinhan.campung.presentation.navigation

import android.content.Intent
import android.util.Log
import androidx.navigation.NavController
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM 알림 클릭 시 네비게이션 처리를 담당하는 Handler
 * MainActivity의 FCM 네비게이션 로직을 중앙화
 */
@Singleton
class FcmNavigationHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "FcmNavigationHandler"
    }
    
    /**
     * FCM Intent에서 네비게이션 데이터를 추출하여 적절한 화면으로 이동
     * @param intent MainActivity로 전달된 Intent
     * @param navController 네비게이션을 수행할 NavController
     */
    fun handleFcmNavigation(intent: Intent, navController: NavController?) {
        Log.d(TAG, "=== FCM Navigation Handler 시작 ===")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent data: ${intent.data}")
        Log.d(TAG, "NavController 상태: ${if (navController != null) "준비됨" else "null"}")
        
        intent.extras?.let { extras ->
            Log.d(TAG, "Intent extras 존재 - 처리 시작")
            logIntentExtras(extras)
            
            // contentId 추출 (다양한 키로 시도)
            val contentId = extractContentId(extras)
            val type = extras.getString("type")
            
            Log.d(TAG, "추출된 데이터 - contentId: $contentId, type: $type")
            
            if (contentId != null && contentId > 0) {
                Log.d(TAG, "ContentDetailScreen으로 네비게이션 시도: $contentId")
                navigateToContent(navController, contentId)
            } else {
                Log.d(TAG, "contentId가 없거나 유효하지 않음: $contentId")
                Log.d(TAG, "네비게이션 실행하지 않음")
            }
        } ?: run {
            Log.d(TAG, "Intent extras가 없음 - FCM 데이터 없음")
        }
        Log.d(TAG, "=== FCM Navigation Handler 완료 ===")
    }
    
    /**
     * 다양한 키로부터 contentId 추출 시도
     * FCM SDK가 자동으로 전달하는 모든 형태를 처리
     */
    private fun extractContentId(extras: android.os.Bundle): Long? {
        // 1. Long 타입으로 전달된 경우 (PostNotificationHandler)
        val longValue = extras.getLong("contentId", 0L).takeIf { it > 0L }
        if (longValue != null) {
            Log.d(TAG, "contentId를 Long으로 추출: $longValue")
            return longValue
        }
        
        // 2. String 타입으로 전달된 경우 (FCM SDK 자동 전달)
        val stringValue = extras.getString("contentId")?.toLongOrNull()
        if (stringValue != null && stringValue > 0) {
            Log.d(TAG, "contentId를 String으로 추출: $stringValue")
            return stringValue
        }
        
        // 3. data 필드에서 JSON 파싱 (백그라운드 FCM)
        val dataField = extras.getString("data")
        if (dataField != null) {
            try {
                Log.d(TAG, "data 필드 발견: $dataField")
                val jsonObject = JSONObject(dataField)
                val contentId = jsonObject.optLong("contentId", 0L)
                if (contentId > 0) {
                    Log.d(TAG, "data JSON에서 contentId 추출 성공: $contentId")
                    return contentId
                }
            } catch (e: Exception) {
                Log.e(TAG, "data JSON 파싱 실패: $dataField", e)
            }
        }
        
        // 4. FCM 네임스페이스 키로 전달된 경우
        val gcmValue = extras.getString("gcm.notification.contentId")?.toLongOrNull()
        if (gcmValue != null && gcmValue > 0) {
            Log.d(TAG, "contentId를 FCM 네임스페이스로 추출: $gcmValue")
            return gcmValue
        }
        
        // 5. Object 타입으로 전달된 경우 (혼합 상황)
        val objectValue = extras.get("contentId")?.toString()?.toLongOrNull()
        if (objectValue != null && objectValue > 0) {
            Log.d(TAG, "contentId를 Object->String으로 추출: $objectValue")
            return objectValue
        }
        
        Log.d(TAG, "contentId를 찾을 수 없음")
        return null
    }
    
    /**
     * ContentDetailScreen으로 네비게이션 수행
     */
    private fun navigateToContent(navController: NavController?, contentId: Long) {
        if (navController == null) {
            Log.w(TAG, "NavController가 준비되지 않음")
            return
        }
        
        try {
            Log.d(TAG, "ContentDetailScreen으로 네비게이션: $contentId")
            navController.navigate("content_detail/$contentId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "네비게이션 실패", e)
        }
    }
    
    /**
     * Intent extras 로깅 (디버깅용)
     */
    private fun logIntentExtras(extras: android.os.Bundle) {
        Log.d(TAG, "Intent extras:")
        for (key in extras.keySet()) {
            Log.d(TAG, "  $key = ${extras.get(key)}")
        }
    }
}