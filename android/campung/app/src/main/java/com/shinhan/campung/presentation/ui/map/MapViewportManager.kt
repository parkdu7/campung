package com.shinhan.campung.presentation.ui.map

import android.util.Log
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.shinhan.campung.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 지도 뷰포트(화면 영역) 변경을 감지하고 관리하는 클래스
 */
class MapViewportManager(
    private val mapViewModel: MapViewModel,
    private val coroutineScope: CoroutineScope
) {
    
    // NaverMap 참조 저장
    private var naverMapRef: NaverMap? = null
    
    private val tag = "MapViewportManager"
    
    // 디바운스를 위한 Job
    private var loadDataJob: Job? = null
    
    // 마지막 요청 정보 (중복 요청 방지)  
    private var lastRequestCenter: LatLng? = null
    private var lastRequestRadius: Int? = null
    
    // 버퍼 로딩을 위한 로드된 영역 정보
    private var currentLoadedArea: LoadedArea? = null
    
    /**
     * 지도 카메라 변경 리스너 생성
     */
    fun createCameraChangeListener(): NaverMap.OnCameraChangeListener {
        return NaverMap.OnCameraChangeListener { reason, animated ->
            Log.d(tag, "카메라 변경 감지 - 이유: $reason, 애니메이션: $animated")
            
            // 모든 카메라 변경에 대해 데이터 로드 스케줄링
            // (네이버 맵 SDK 버전에 따라 상수명이 다를 수 있으므로 단순화)
            Log.d(tag, "카메라 변경 감지 - 이유: $reason")
            scheduleDataLoad()
        }
    }
    
    /**
     * 데이터 로드 스케줄링 (버퍼 기반 로직)
     */
    private fun scheduleDataLoad() {
        val naverMap = getCurrentNaverMap() ?: return
        val currentCenter = naverMap.cameraPosition.target
        
        // 현재 화면이 로드된 영역을 벗어났는지 확인
        if (!MapBoundsCalculator.isOutOfLoadedArea(currentCenter, currentLoadedArea)) {
            Log.d(tag, "현재 화면이 로드된 영역 내에 있음 - 로딩 스킵")
            return
        }
        
        // 이전 Job 취소
        loadDataJob?.cancel()
        
        // 새로운 Job 시작 (적응형 디바운스)
        val debounceDelay = 100L // 일정한 디바운스 적용
        
        loadDataJob = coroutineScope.launch {
            delay(debounceDelay)
            
            try {
                loadMapContentsForCurrentViewWithBuffer()
            } catch (e: Exception) {
                Log.e(tag, "데이터 로드 중 에러 발생", e)
            }
        }
    }
    
    /**
     * 현재 화면 영역에 대한 맵 데이터 로드 (버퍼 포함)
     */
    private suspend fun loadMapContentsForCurrentViewWithBuffer() {
        val naverMap = getCurrentNaverMap() ?: run {
            Log.w(tag, "NaverMap 인스턴스가 null입니다")
            return
        }
        
        // 현재 화면 중앙과 버퍼가 포함된 반경 계산
        val center = naverMap.cameraPosition.target
        val bufferedRadius = MapBoundsCalculator.calculateBufferedRadius(naverMap)
        
        Log.d(tag, "버퍼 데이터 로드 요청 - 중심: (${center.latitude}, ${center.longitude}), 버퍼반경: ${bufferedRadius}m")
        
        // 요청 정보 저장
        lastRequestCenter = center
        lastRequestRadius = bufferedRadius
        
        // 로드된 영역 정보 업데이트
        currentLoadedArea = LoadedArea(center, bufferedRadius.toDouble())
        
        // 디버깅 정보 출력
        val areaInfo = MapBoundsCalculator.getVisibleAreaInfo(naverMap)
        Log.v(tag, areaInfo.toString())
        
        // ViewModel에 데이터 로드 요청 (버퍼 반경으로)
        mapViewModel.loadMapContents(
            latitude = center.latitude,
            longitude = center.longitude,
            radius = bufferedRadius
        )
        
        // POI 데이터는 화면 반경으로 업데이트 (버퍼 적용하지 않음)
        val visibleRadius = MapBoundsCalculator.calculateVisibleRadius(naverMap)
        Log.v(tag, "🏪 화면 변경으로 POI 업데이트 요청")
        mapViewModel.updatePOIForLocation(center.latitude, center.longitude, visibleRadius)
    }
    
    /**
     * 현재 화면 영역에 대한 맵 데이터 로드 (기존 버전 - 호환성 유지)
     */
    private suspend fun loadMapContentsForCurrentView() {
        val naverMap = getCurrentNaverMap() ?: run {
            Log.w(tag, "NaverMap 인스턴스가 null입니다")
            return
        }
        
        // 현재 화면 중앙과 반경 계산
        val center = naverMap.cameraPosition.target
        val radius = MapBoundsCalculator.calculateVisibleRadius(naverMap)
        
        Log.d(tag, "데이터 로드 요청 - 중심: (${center.latitude}, ${center.longitude}), 반경: ${radius}m")
        
        // 중복 요청 체크
        if (isDuplicateRequest(center, radius)) {
            Log.d(tag, "중복 요청으로 스킵")
            return
        }
        
        // 요청 정보 저장
        lastRequestCenter = center
        lastRequestRadius = radius
        
        // 디버깅 정보 출력
        val areaInfo = MapBoundsCalculator.getVisibleAreaInfo(naverMap)
        Log.v(tag, areaInfo.toString())
        
        // ViewModel에 데이터 로드 요청
        mapViewModel.loadMapContents(
            latitude = center.latitude,
            longitude = center.longitude,
            radius = radius
        )
        
        // POI 데이터도 함께 업데이트 (POI가 활성화된 경우)
        Log.v(tag, "🏪 화면 변경으로 POI 업데이트 요청")
        mapViewModel.updatePOIForLocation(center.latitude, center.longitude, radius)
    }
    
    /**
     * 중복 요청 체크 - 더 관대하게 수정
     */
    private fun isDuplicateRequest(center: LatLng, radius: Int): Boolean {
        val lastCenter = lastRequestCenter ?: return false
        val lastRadius = lastRequestRadius ?: return false
        
        // 중심점 이동 거리 계산 (50m 이내면 중복으로 간주 - 더 짧게)  
        val distance = MapBoundsCalculator.calculateDistance(lastCenter, center)
        val radiusDiff = kotlin.math.abs(radius - lastRadius)
        
        val isDuplicate = distance < 50 && radiusDiff < radius * 0.05 // 반경 변화 5% 이내
        
        if (isDuplicate) {
            Log.d(tag, "중복 요청 감지 - 거리: ${distance.toInt()}m, 반경차이: ${radiusDiff}m")
        }
        
        return isDuplicate
    }
    
    /**
     * 수동으로 현재 화면 영역 데이터 로드 (최초 로드 등)
     */
    fun loadCurrentViewData() {
        Log.d(tag, "수동 데이터 로드 요청 (버퍼 포함)")
        loadDataJob?.cancel()
        
        loadDataJob = coroutineScope.launch {
            loadMapContentsForCurrentViewWithBuffer()
        }
    }
    
    /**
     * 강제로 데이터 재로드 (필터 변경 등)
     */
    fun forceReload() {
        Log.d(tag, "강제 데이터 재로드")
        currentLoadedArea = null // 로드된 영역 초기화
        loadCurrentViewData()
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        Log.d(tag, "MapViewportManager 정리")
        loadDataJob?.cancel()
        lastRequestCenter = null
        lastRequestRadius = null
        currentLoadedArea = null
    }
    
    /**
     * NaverMap 참조 설정
     */
    fun setNaverMap(naverMap: NaverMap) {
        this.naverMapRef = naverMap
        Log.d(tag, "NaverMap 참조 설정됨")
    }
    
    // NaverMap 인스턴스를 가져오는 함수
    private fun getCurrentNaverMap(): NaverMap? {
        return naverMapRef
    }
    
}