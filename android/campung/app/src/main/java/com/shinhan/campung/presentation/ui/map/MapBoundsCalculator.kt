package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.NaverMap
import kotlin.math.*

/**
 * 지도 화면 영역을 기반으로 반경을 계산하는 유틸리티 클래스
 */
object MapBoundsCalculator {
    
    private const val TAG = "MapBoundsCalculator"
    
    /**
     * 현재 화면에 표시되는 영역을 기반으로 API 요청용 반경을 계산
     * @param naverMap 네이버 지도 인스턴스
     * @param marginRatio 여유분 비율 (기본 20%)
     * @return 반경(미터)
     */
    fun calculateVisibleRadius(naverMap: NaverMap, marginRatio: Double = 0.2): Int {
        val bounds = naverMap.contentBounds
        Log.d(TAG, "화면 경계: $bounds")
        
        // 화면 중앙점
        val center = naverMap.cameraPosition.target
        Log.d(TAG, "화면 중앙: $center")
        
        // 화면 모서리까지의 거리들 계산
        val distances = listOf(
            // 중앙에서 각 모서리까지의 거리
            calculateDistance(center, LatLng(bounds.northEast.latitude, center.longitude)), // 북쪽
            calculateDistance(center, LatLng(bounds.southWest.latitude, center.longitude)), // 남쪽  
            calculateDistance(center, LatLng(center.latitude, bounds.northEast.longitude)), // 동쪽
            calculateDistance(center, LatLng(center.latitude, bounds.southWest.longitude))  // 서쪽
        )
        
        // 가장 먼 거리를 기준으로 계산
        val maxDistance = distances.maxOrNull() ?: 1000.0
        Log.d(TAG, "최대 거리: ${maxDistance}m")
        
        // 여유분 추가
        val radiusWithMargin = (maxDistance * (1 + marginRatio)).toInt()
        
        // 최소/최대 반경 제한
        val finalRadius = radiusWithMargin.coerceIn(MIN_RADIUS, MAX_RADIUS)
        
        Log.d(TAG, "계산된 반경: ${finalRadius}m (원본: ${maxDistance}m, 여유분: ${marginRatio * 100}%)")
        return finalRadius
    }
    
    /**
     * 줌 레벨 기반 예상 반경 계산 (참고용)
     */
    fun getExpectedRadiusByZoom(zoom: Double): Int {
        return when {
            zoom >= 19 -> 100    // 매우 상세한 뷰
            zoom >= 17 -> 300    // 상세한 뷰
            zoom >= 15 -> 800    // 일반적인 뷰  
            zoom >= 13 -> 2000   // 넓은 뷰
            zoom >= 11 -> 5000   // 매우 넓은 뷰
            else -> 10000        // 광역 뷰
        }.coerceIn(MIN_RADIUS, MAX_RADIUS)
    }
    
    /**
     * 두 좌표 사이의 거리 계산 (하버사인 공식)
     */
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * 화면 영역 정보 반환 (디버깅용)
     */
    fun getVisibleAreaInfo(naverMap: NaverMap): VisibleAreaInfo {
        val bounds = naverMap.contentBounds
        val center = naverMap.cameraPosition.target
        val zoom = naverMap.cameraPosition.zoom
        val radius = calculateVisibleRadius(naverMap)
        
        return VisibleAreaInfo(
            center = center,
            bounds = bounds,
            zoom = zoom,
            calculatedRadius = radius,
            expectedRadius = getExpectedRadiusByZoom(zoom)
        )
    }
    
    // 반경 제한값 (서버 성능 및 사용자 경험 고려)
    private const val MIN_RADIUS = 100   // 최소 100m
    private const val MAX_RADIUS = 50000 // 최대 50km
}

/**
 * 화면 영역 정보를 담는 데이터 클래스
 */
data class VisibleAreaInfo(
    val center: LatLng,
    val bounds: LatLngBounds,
    val zoom: Double,
    val calculatedRadius: Int,
    val expectedRadius: Int
) {
    override fun toString(): String {
        return """
            |VisibleAreaInfo:
            |  중심좌표: ${center.latitude}, ${center.longitude}
            |  줌레벨: $zoom
            |  계산된반경: ${calculatedRadius}m
            |  예상반경: ${expectedRadius}m
            |  경계: ${bounds.southWest} ~ ${bounds.northEast}
        """.trimMargin()
    }
}