package com.shinhan.campung.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.*
import com.shinhan.campung.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap

/**
 * 내 위치를 표시하는 Lottie 애니메이션 마커
 * 지도 카메라 변경에 반응하여 실시간으로 위치를 업데이트함
 */
@Composable
fun MyLocationMarker(
    map: NaverMap,
    location: LatLng,
    modifier: Modifier = Modifier
) {
    // 화면 좌표를 실시간으로 추적
    var screenCoord by remember(location) { 
        mutableStateOf(map.projection.toScreenLocation(location)) 
    }
    
    // 카메라 변경 시 화면 좌표 업데이트
    DisposableEffect(map, location) {
        val cameraListener = NaverMap.OnCameraChangeListener { _, _ ->
            screenCoord = map.projection.toScreenLocation(location)
        }
        
        map.addOnCameraChangeListener(cameraListener)
        
        onDispose {
            map.removeOnCameraChangeListener(cameraListener)
        }
    }
    
    // Lottie 애니메이션 설정
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.my_location_animation)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        restartOnPlay = true,
        speed = 1.0f
    )
    
    // 화면에 마커 표시
    Box(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { (screenCoord.x - 55f).toDp() },
                y = with(LocalDensity.current) { (screenCoord.y - 55f).toDp() }
            )
            .size(70.dp)
            .zIndex(0f) // 지도 위에, 하지만 마커들 뒤에
    ) {
        composition?.let { comp ->
            // Lottie 애니메이션 성공적으로 로드됨
            LottieAnimation(
                composition = comp,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            // Lottie 로드 실패 시 대체 UI
            FallbackLocationMarker()
        }
    }
}

/**
 * Lottie 애니메이션 로드 실패 시 사용할 대체 마커
 */
@Composable
private fun FallbackLocationMarker() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val center = androidx.compose.ui.geometry.Offset(
            x = size.width / 2,
            y = size.height / 2
        )
        
        // 외곽 펄싱 원 (투명도 효과)
        drawCircle(
            color = Color.Blue.copy(alpha = 0.2f),
            radius = 20.dp.toPx(),
            center = center
        )
        
        // 중간 원
        drawCircle(
            color = Color.Blue.copy(alpha = 0.5f),
            radius = 12.dp.toPx(),
            center = center
        )
        
        // 중심 원
        drawCircle(
            color = Color.Blue,
            radius = 6.dp.toPx(),
            center = center
        )
        
        // 중심점 (흰색)
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

