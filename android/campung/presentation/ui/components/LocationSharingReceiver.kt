package com.shinhan.campung.presentation.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.shinhan.campung.data.service.LocationSharingManager

/**
 * 위치 공유 브로드캐스트를 수신하는 Composable
 * @param locationSharingManager 위치 공유 매니저
 */
@Composable
fun LocationSharingReceiver(
    locationSharingManager: LocationSharingManager
) {
    val context = LocalContext.current
    
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.shinhan.campung.LOCATION_SHARED") {
                    val userName = intent.getStringExtra("userName") ?: return
                    val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull() ?: return
                    val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull() ?: return
                    val displayUntil = intent.getStringExtra("displayUntil") ?: return
                    val shareId = intent.getStringExtra("shareId") ?: return
                    
                    locationSharingManager.addSharedLocation(
                        userName, latitude, longitude, displayUntil, shareId
                    )
                }
            }
        }
        
        val intentFilter = IntentFilter("com.shinhan.campung.LOCATION_SHARED")
        context.registerReceiver(receiver, intentFilter)
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // 이미 unregister된 경우 무시
            }
        }
    }
}