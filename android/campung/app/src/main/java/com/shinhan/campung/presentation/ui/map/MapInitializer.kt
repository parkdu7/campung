package com.shinhan.campung.presentation.ui.map

import com.naver.maps.map.NaverMap
import com.naver.maps.map.MapView
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import android.widget.ImageView
import com.shinhan.campung.R
import android.os.Handler


class MapInitializer {
    
    private var myLocationView: ImageView? = null
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    
    // 무한 루프 방지를 위한 플래그
    private var isAdjustingCamera = false
    
    fun setupMapUI(map: NaverMap) {
        // 커스텀 스타일 설정 - SDK 3.22.1에서는 setStyle 메서드 사용
        try {
            map.setCustomStyleId("258120eb-1ebf-4b29-97cf-21df68e09c5c")
        } catch (e: Exception) {
            // 스타일 적용 실패 시 기본 스타일 유지
            e.printStackTrace()
        }
        
        // 대한민국 영역으로 이동 제한 설정
        setupKoreaMovementRestriction(map)
        
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
    
    /**
     * 네이버 지도 SDK의 extent 속성과 줌 레벨 제한을 사용한 한국 지역 카메라 제한
     */
    private fun setupKoreaMovementRestriction(map: NaverMap) {
        try {
            // 네이버 지도 공식 문서 권장 한국 반도 경계 좌표 (정확한 값 사용)
            val koreaBounds = LatLngBounds(
                LatLng(31.43, 122.37), // 남서쪽 (제주도 남쪽 포함)
                LatLng(44.35, 132.0)   // 북동쪽 (북한 북쪽 + 독도 포함)
            )
            
            // extent 속성으로 지도 이동 범위 제한 (네이버 지도 SDK 공식 방법)
            map.extent = koreaBounds
            android.util.Log.d("MapInitializer", "✅ extent 속성으로 대한민국 이동 제한 설정: $koreaBounds")
            
            // 줌 레벨 제한 설정 (한국 전체가 적절히 보이도록)
            map.minZoom = 6.0   // 최소 줌 (한국 전체가 화면에 보이는 수준)
            map.maxZoom = 21.0  // 최대 줌 (상세 지역까지 확대 가능)
            android.util.Log.d("MapInitializer", "✅ 줌 레벨 제한 설정: minZoom=${map.minZoom}, maxZoom=${map.maxZoom}")
            
        } catch (e: Exception) {
            android.util.Log.e("MapInitializer", "카메라 제한 설정 실패", e)
            // 실패 시에도 기본 줌 레벨은 설정 시도
            try {
                map.minZoom = 6.0
                map.maxZoom = 21.0
                android.util.Log.d("MapInitializer", "⚠️ extent 실패했지만 줌 레벨 제한은 적용됨")
            } catch (zoomException: Exception) {
                android.util.Log.e("MapInitializer", "줌 레벨 제한도 설정 실패", zoomException)
            }
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