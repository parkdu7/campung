package com.shinhan.campung.presentation.ui.components

import androidx.compose.runtime.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap

/**
 * FullMapScreen에서 사용할 위치 마커 컴포넌트
 * LocationMarkerManager를 활용한 예시
 */
@Composable
fun FullMapLocationMarker(
    map: NaverMap?,
    userLocation: LatLng?,
    hasLocationPermission: Boolean,
    enableAnimation: Boolean = true
) {
    // 모듈화된 LocationMarkerManager 사용
    LocationMarkerManager(
        map = map,
        userLocation = userLocation,
        hasLocationPermission = hasLocationPermission,
        enableLottieAnimation = enableAnimation
    )
}

/**
 * 위치 마커를 조건부로 표시하는 컴포넌트
 * 더 세밀한 제어가 필요한 경우 사용
 */
@Composable
fun ConditionalLocationMarker(
    map: NaverMap?,
    userLocation: LatLng?,
    hasLocationPermission: Boolean,
    showMarker: Boolean = true,
    config: LocationMarkerConfig = LocationMarkerConfig()
) {
    if (showMarker) {
        LocationMarkerManager(
            map = map,
            userLocation = userLocation,
            hasLocationPermission = hasLocationPermission,
            enableLottieAnimation = config.enableLottieAnimation
        )
    } else {
        // 마커 숨기기
        LaunchedEffect(showMarker) {
            map?.locationOverlay?.isVisible = false
        }
    }
}