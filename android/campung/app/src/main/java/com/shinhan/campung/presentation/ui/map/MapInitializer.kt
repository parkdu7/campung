package com.shinhan.campung.presentation.ui.map

import com.naver.maps.map.NaverMap
import com.naver.maps.map.MapView
import android.widget.ImageView
import com.shinhan.campung.R
import android.os.Handler


class MapInitializer {
    
    private var myLocationView: ImageView? = null
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    
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
            isCompassEnabled = false
            isLocationButtonEnabled = false
            setLogoMargin(-500, -500, 0, 0)
        }
    }
    
    fun setupLocationOverlay(
        map: NaverMap,
        mapView: MapView,
        hasPermission: Boolean,
        myLatLng: com.naver.maps.geometry.LatLng?
    ) {
        // 기본 LocationOverlay 완전히 비활성화 (Compose Lottie 애니메이션 사용)
        map.locationOverlay.isVisible = false
        
        // 추가 확실한 비활성화 설정
        try {
            map.locationOverlay.map = null
        } catch (e: Exception) {
            // 이미 비활성화된 경우 무시
        }
        
        // 기존 커스텀 뷰들 정리
        myLocationView?.let { 
            try {
                mapView.removeView(it)
            } catch (e: Exception) {
                // 이미 제거된 경우 무시
            }
            myLocationView = null
        }
        
        // 핸들러 정리
        animationRunnable?.let { runnable ->
            animationHandler?.removeCallbacks(runnable)
        }
        animationHandler = null
        animationRunnable = null
        
        android.util.Log.d("MapInitializer", "LocationOverlay 완전히 비활성화 완료")
    }
}