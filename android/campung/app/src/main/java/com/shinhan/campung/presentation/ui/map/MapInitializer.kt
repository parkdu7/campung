package com.shinhan.campung.presentation.ui.map

import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapOptions

class MapInitializer {
    
    fun setupMapUI(map: NaverMap) {
        // 커스텀 스타일 설정 - SDK 3.22.1에서는 setStyle 메서드 사용
        try {
            map.setCustomStyleId("258120eb-1ebf-4b29-97cf-21df68e09c5c")
        } catch (e: Exception) {
            // 스타일 적용 실패 시 기본 스타일 유지
            e.printStackTrace()
        }
        
        map.uiSettings.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
            isZoomControlEnabled = false
            isScaleBarEnabled = false
            isCompassEnabled = true
            isLocationButtonEnabled = false
        }
    }
    
    fun setupLocationOverlay(
        map: NaverMap,
        hasPermission: Boolean,
        myLatLng: com.naver.maps.geometry.LatLng?
    ) {
        if (myLatLng != null && hasPermission) {
            map.locationOverlay.isVisible = true
            map.locationOverlay.position = myLatLng
        } else {
            map.locationOverlay.isVisible = false
        }
    }
}