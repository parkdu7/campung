package com.shinhan.campung

import android.os.Build
import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.google.firebase.messaging.FirebaseMessaging
import com.naver.maps.map.MapView
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.navigation.AppNav
import com.shinhan.campung.presentation.navigation.FcmNavigationHandler
import com.shinhan.campung.presentation.ui.theme.CampungTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authDataStore: AuthDataStore
    @Inject lateinit var fcmNavigationHandler: FcmNavigationHandler
    
    // NavController 참조를 저장
    private var navController: NavController? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한이 허용되었을 때 추가 작업
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "=== MainActivity onCreate 시작 ===")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }

        requestNotificationPermission()
        
        Log.d("MainActivity", "onCreate Intent 확인: ${intent}")
        Log.d("MainActivity", "onCreate Intent extras: ${intent.extras}")
        
        // FCM 라우트 추출 (onCreate에서만)
        val fcmRoute = extractFcmRoute(intent)
        Log.d("MainActivity", "추출된 FCM 라우트: $fcmRoute")
        
        // onCreate에서 FCM 라우트가 있다면 setupFirebaseMessaging 호출하지 않음
        if (fcmRoute == null) {
            setupFirebaseMessaging()
        } else {
            Log.d("MainActivity", "FCM 라우트로 직접 시작하므로 setupFirebaseMessaging 생략")
        }
        
        Log.d("MainActivity", "=== MainActivity onCreate 완료 ===")

        // ✅ MapView를 Activity 레벨에서 한 번만 생성
        val sharedMapView = MapView(this).apply { onCreate(null) }

        setContent {
            CampungTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(
                        authDataStore = authDataStore, 
                        sharedMapView = sharedMapView,
                        onNavControllerReady = { navController = it },
                        initialRoute = fcmRoute
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "=== onNewIntent 호출 ===")
        Log.d("MainActivity", "새로운 Intent: $intent")
        Log.d("MainActivity", "새로운 Intent extras: ${intent.extras}")
        
        setIntent(intent)  // 새로운 Intent를 Activity에 설정
        
        // 앱이 이미 실행 중일 때 FCM 알림 클릭 처리 (기존 방식 유지)
        handleFcmIntent(intent)
        Log.d("MainActivity", "=== onNewIntent 완료 ===")
    }

    /**
     * Intent에서 FCM 데이터를 추출하여 라우트 생성
     */
    private fun extractFcmRoute(intent: Intent): String? {
        intent.extras?.let { extras ->
            Log.d("MainActivity", "FCM 라우트 추출 중...")
            
            // contentId 추출 (FcmNavigationHandler와 동일한 로직 사용)
            val contentId = extractContentIdFromExtras(extras)
            val type = extras.getString("type")
            
            Log.d("MainActivity", "추출된 데이터 - contentId: $contentId, type: $type")
            
            if (contentId != null && contentId > 0) {
                Log.d("MainActivity", "FCM 라우트 생성: content_detail/$contentId")
                return "content_detail/$contentId"
            }
        }
        
        Log.d("MainActivity", "FCM 라우트 없음")
        return null
    }
    
    /**
     * Bundle에서 contentId 추출 (FcmNavigationHandler와 동일한 로직)
     */
    private fun extractContentIdFromExtras(extras: android.os.Bundle): Long? {
        // 1. Long 타입으로 전달된 경우
        val longValue = extras.getLong("contentId", 0L).takeIf { it > 0L }
        if (longValue != null) {
            Log.d("MainActivity", "contentId를 Long으로 추출: $longValue")
            return longValue
        }
        
        // 2. String 타입으로 전달된 경우
        val stringValue = extras.getString("contentId")?.toLongOrNull()
        if (stringValue != null && stringValue > 0) {
            Log.d("MainActivity", "contentId를 String으로 추출: $stringValue")
            return stringValue
        }
        
        // 3. Object 타입으로 전달된 경우
        val objectValue = extras.get("contentId")?.toString()?.toLongOrNull()
        if (objectValue != null && objectValue > 0) {
            Log.d("MainActivity", "contentId를 Object로 추출: $objectValue")
            return objectValue
        }
        
        Log.d("MainActivity", "contentId를 찾을 수 없음")
        return null
    }
    
    private fun setupFirebaseMessaging() {
        // onCreate에서 FCM 라우트가 없는 경우에만 호출됨
        // 일반적인 앱 시작 시 FCM 관련 초기화가 필요하다면 여기에 추가
        Log.d("MainActivity", "일반적인 Firebase 초기화")
    }
    
    private fun handleFcmIntent(intent: Intent) {
        Log.d("MainActivity", "FCM Intent 처리 시작")
        
        // NavController가 준비되었는지 확인
        if (navController != null) {
            Log.d("MainActivity", "NavController 준비됨 - 즉시 처리")
            Log.d("MainActivity", "fcmNavigationHandler 인스턴스: $fcmNavigationHandler")
            Log.d("MainActivity", "즉시 handleFcmNavigation 호출 시작")
            fcmNavigationHandler.handleFcmNavigation(intent, navController)
            Log.d("MainActivity", "즉시 handleFcmNavigation 호출 완료")
        } else {
            Log.d("MainActivity", "NavController 미준비 - 대기 후 처리")
            // NavController 준비 대기
            android.os.Handler(mainLooper).postDelayed({
                Log.d("MainActivity", "대기 후 FCM 처리 실행")
                Log.d("MainActivity", "fcmNavigationHandler 인스턴스: $fcmNavigationHandler")
                Log.d("MainActivity", "navController 상태: $navController")
                Log.d("MainActivity", "handleFcmNavigation 호출 시작")
                fcmNavigationHandler.handleFcmNavigation(intent, navController)
                Log.d("MainActivity", "handleFcmNavigation 호출 완료")
            }, 700) // NavController 준비를 위한 0.7초 대기
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PermissionChecker.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}