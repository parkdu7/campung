package com.shinhan.campung

import android.os.Build
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.naver.maps.map.MapView
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.navigation.AppNav
import com.shinhan.campung.presentation.ui.theme.CampungTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authDataStore: AuthDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한이 허용되었을 때 추가 작업
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }

        requestNotificationPermission()

        // ✅ MapView를 Activity 레벨에서 한 번만 생성
        val sharedMapView = MapView(this).apply { onCreate(null) }

        setContent {
            CampungTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNav(authDataStore, sharedMapView) // AppNav로 전달
                }
            }
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