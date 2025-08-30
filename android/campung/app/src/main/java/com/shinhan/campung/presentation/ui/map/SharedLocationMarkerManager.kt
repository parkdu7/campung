package com.shinhan.campung.presentation.ui.map

import androidx.compose.ui.graphics.Color
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.shinhan.campung.data.model.SharedLocation
import com.shinhan.campung.R
import android.util.Log

/**
 * 위치 공유 마커들을 관리하는 클래스
 */
class SharedLocationMarkerManager {
    
    companion object {
        private const val TAG = "SharedLocationMarkerManager"
    }
    
    private var sharedLocationMarkers = mutableMapOf<String, Marker>()
    
    /**
     * 위치 공유 마커들 업데이트
     * @param naverMap 네이버 맵 인스턴스
     * @param locations 공유된 위치 목록
     */
    fun updateSharedLocationMarkers(naverMap: NaverMap, locations: List<SharedLocation>) {
        Log.d(TAG, "updateSharedLocationMarkers 호출됨 - 위치 개수: ${locations.size}")
        
        // 중복 제거: 같은 userName의 경우 가장 최근 displayUntil을 가진 것만 유지
        val deduplicatedLocations = locations
            .groupBy { it.userName } // userName으로 그룹화
            .mapValues { (_, userLocations) ->
                // 각 사용자별로 displayUntil이 가장 늦은(최신) 위치만 선택
                userLocations.maxByOrNull { it.displayUntil }
            }
            .values
            .filterNotNull()
        
        Log.d(TAG, "중복 제거 완료 - 원본: ${locations.size}개 → 필터링 후: ${deduplicatedLocations.size}개")
        
        // 기존 마커 제거
        Log.d(TAG, "기존 마커 ${sharedLocationMarkers.size}개 제거 중")
        clearAllMarkers()
        
        // 새 마커 생성 (중복 제거된 목록으로)
        deduplicatedLocations.forEachIndexed { index, sharedLocation ->
            Log.d(TAG, "[$index] 마커 생성 중: ${sharedLocation.userName} at (${sharedLocation.latitude}, ${sharedLocation.longitude}) until ${sharedLocation.displayUntil}")
            createSharedLocationMarker(naverMap, sharedLocation)
        }
        
        Log.d(TAG, "마커 업데이트 완료 - 현재 마커 개수: ${sharedLocationMarkers.size}")
    }
    
    /**
     * 개별 위치 공유 마커 생성
     */
    private fun createSharedLocationMarker(naverMap: NaverMap, sharedLocation: SharedLocation) {
        try {
            Log.d(TAG, "마커 생성 시작: shareId=${sharedLocation.shareId}, userName=${sharedLocation.userName}")
            
            val marker = Marker().apply {
                position = LatLng(sharedLocation.latitude, sharedLocation.longitude)
                captionText = "${sharedLocation.userName}님의 위치"
                captionTextSize = 12f
                captionColor = 0xFF0000FF.toInt() // 파란색 고정값
                width = 140
                height = 140
                // location_share 아이콘 적용
                icon = OverlayImage.fromResource(R.drawable.location_share)
                map = naverMap
                
                Log.d(TAG, "마커 속성 설정 완료 - 위치: (${sharedLocation.latitude}, ${sharedLocation.longitude})")
                
                // 마커 클릭 이벤트 (선택사항)
                setOnClickListener {
                    Log.d(TAG, "마커 클릭됨: ${sharedLocation.userName}")
                    true
                }
            }
            
            sharedLocationMarkers[sharedLocation.shareId] = marker
            Log.d(TAG, "마커 생성 완료 및 저장됨: shareId=${sharedLocation.shareId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "마커 생성 실패: shareId=${sharedLocation.shareId}", e)
        }
    }
    
    /**
     * 특정 위치 공유 마커 제거
     */
    fun removeSharedLocationMarker(shareId: String) {
        sharedLocationMarkers[shareId]?.let { marker ->
            marker.map = null
            marker.onClickListener = null // 클릭 리스너 제거
            marker.tag = null // 태그 제거
            sharedLocationMarkers.remove(shareId)
            Log.d(TAG, "위치 공유 마커 제거 완료: shareId=$shareId")
        }
    }
    
    /**
     * 모든 위치 공유 마커 제거
     */
    fun clearAllMarkers() {
        val count = sharedLocationMarkers.size
        sharedLocationMarkers.values.forEach { marker ->
            marker.map = null
            marker.onClickListener = null // 클릭 리스너 제거
            marker.tag = null // 태그 제거
        }
        sharedLocationMarkers.clear()
        Log.d(TAG, "위치 공유 마커 ${count}개 모두 제거됨 (리스너 및 태그 정리 완료)")
    }
    
    /**
     * 현재 표시된 마커 개수 반환
     */
    fun getMarkerCount(): Int = sharedLocationMarkers.size
    
    /**
     * 특정 shareId의 마커가 존재하는지 확인
     */
    fun hasMarker(shareId: String): Boolean = sharedLocationMarkers.containsKey(shareId)
}