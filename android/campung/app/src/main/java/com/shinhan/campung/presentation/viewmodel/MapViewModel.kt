package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.repository.MapContentRepository
import com.shinhan.campung.data.repository.MapRepository
import com.shinhan.campung.data.mapper.ContentMapper
import com.shinhan.campung.data.model.ContentCategory
import com.shinhan.campung.presentation.ui.components.TooltipState
import com.shinhan.campung.presentation.ui.components.TooltipType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.shinhan.campung.data.service.LocationSharingManager
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapContentRepository: MapContentRepository,
    private val mapRepository: MapRepository,
    private val contentMapper: ContentMapper,
    val locationSharingManager: LocationSharingManager // public으로 노출
) : BaseViewModel() {
    fun getLastKnownLocation(): Pair<Double, Double>? = lastRequestLocation

    // UI States
    private val _bottomSheetContents = MutableStateFlow<List<MapContent>>(emptyList())
    val bottomSheetContents: StateFlow<List<MapContent>> = _bottomSheetContents.asStateFlow()

    private val _selectedMarkerId = MutableStateFlow<Long?>(null)
    val selectedMarkerId: StateFlow<Long?> = _selectedMarkerId.asStateFlow()

    private val _isBottomSheetExpanded = MutableStateFlow(false)
    val isBottomSheetExpanded: StateFlow<Boolean> = _isBottomSheetExpanded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 툴팁 상태 관리
    private val _tooltipState = MutableStateFlow(TooltipState())
    val tooltipState: StateFlow<TooltipState> = _tooltipState.asStateFlow()
    
    // 위치 공유 상태를 LocationSharingManager에서 가져옴
    val sharedLocations: StateFlow<List<com.shinhan.campung.data.model.SharedLocation>> = 
        locationSharingManager.sharedLocations

    // 마커 클릭 처리 (자연스러운 바텀시트)
    fun onMarkerClick(contentId: Long, associatedContentIds: List<Long>) {
        _selectedMarkerId.value = contentId
        _isLoading.value = true
        // 로딩 상태에서 즉시 바텀시트 확장 (로딩 UI 표시)
        _isBottomSheetExpanded.value = true

        // 데이터 로드
        viewModelScope.launch {
            mapContentRepository.getContentsByIds(associatedContentIds)
                .onSuccess { contents ->
                    _bottomSheetContents.value = contents
                    // 컨텐츠가 준비되더라도 이미 확장된 상태 유지
                }
                .onFailure {
                    _bottomSheetContents.value = emptyList()
                    _isBottomSheetExpanded.value = false  // 실패시만 바텀시트 닫기
                }
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
    }

    var mapContents by mutableStateOf<List<MapContent>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var shouldUpdateClustering by mutableStateOf(false)
        private set

    // 선택된 마커 상태 추가
    var selectedMarker by mutableStateOf<MapContent?>(null)
        private set

    // 필터 상태
    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var selectedTags by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedPostType by mutableStateOf("ALL")
        private set

    private var debounceJob: Job? = null
    private var lastRequestLocation: Pair<Double, Double>? = null
    private var lastRequestParams: RequestParams? = null

    private data class RequestParams(
        val location: Pair<Double, Double>,
        val date: LocalDate,
        val tags: Set<String>,
        val postType: String
    )

    private var pendingHighlightId: Long? = null

    fun requestHighlight(contentId: Long) {
        pendingHighlightId = contentId
    }

    fun loadMapContents(
        latitude: Double,
        longitude: Double,
        radius: Int? = null,
        postType: String? = null,
        force: Boolean = false                 // ✅ 추가
    ) {
        // 이전 요청 취소
        debounceJob?.cancel()

        val currentLocation = Pair(latitude, longitude)
        val currentParams = RequestParams(
            location = currentLocation,
            date = selectedDate,
            tags = selectedTags,
            postType = postType ?: selectedPostType
        )

//        // 이전 요청과 비교해서 중복 요청 방지
//        lastRequestParams?.let { lastParams ->
//            val locationDistance = calculateDistance(
//                lastParams.location.first, lastParams.location.second,
//                latitude, longitude
//            )
//
//            // 위치는 같고 (500m 이내), 다른 파라미터도 동일하면 스킵
//            if (locationDistance < 500.0 &&
//                lastParams.date == currentParams.date &&
//                lastParams.tags == currentParams.tags &&
//                lastParams.postType == currentParams.postType) {
//                return
//            }
//        }

        // ✅ 중복 요청 스킵 로직 우회 (force=true이면 건너뜀)
        if (!force) {
            lastRequestParams?.let { lastParams ->
                val locationDistance = calculateDistance(
                    lastParams.location.first, lastParams.location.second,
                    latitude, longitude
                )
                if (locationDistance < 500.0 &&
                    lastParams.date == currentParams.date &&
                    lastParams.tags == currentParams.tags &&
                    lastParams.postType == currentParams.postType) {
                    return
                }
            }
        }

        // 100ms 디바운스 적용 (빠른 반응)
        debounceJob = viewModelScope.launch {
            delay(100)

            _isLoading.value = true
            errorMessage = null
            lastRequestLocation = currentLocation
            lastRequestParams = currentParams

            try {
                val dateString = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val requestRadius = radius ?: getDefaultRadius()

                val response = mapRepository.getMapContents(
                    latitude = latitude,
                    longitude = longitude,
                    radius = requestRadius,
                    postType = postType ?: selectedPostType,
                    date = dateString
                ).getOrThrow()

                if (response.success) {
                    mapContents = response.data.contents
                    shouldUpdateClustering = true

                    // ✅ 방금 등록한 ID가 있으면 자동으로 선택/하이라이트
                    pendingHighlightId?.let { id ->
                        mapContents.firstOrNull { it.contentId == id }?.let { selectMarker(it) }
                        pendingHighlightId = null
                    }

                    selectedMarker?.let { selected ->
                        val stillExists = mapContents.any { it.contentId == selected.contentId }
                        if (!stillExists) selectedMarker = null
                    }
                } else {
                    errorMessage = response.message
                }

                _isLoading.value = false
            } catch (t: Throwable) {
                errorMessage = t.message ?: "알 수 없는 오류가 발생했습니다"
                _isLoading.value = false
            }
        }
    }

    // 마커 선택 상태 관리 함수들
    fun selectMarker(mapContent: MapContent) {
        Log.e(TAG, "🎯🎯🎯 [FLOW] selectMarker() 시작: ${mapContent.title} (ID: ${mapContent.contentId})")
        Log.d(TAG, "🎯 [FLOW] selectMarker() 시작: ${mapContent.title} (ID: ${mapContent.contentId})")
        
        selectedMarker = mapContent
        Log.d(TAG, "📌 [FLOW] selectedMarker 상태 업데이트 완료")
        
        // 바텀시트 데이터 로딩 - 기존 onMarkerClick 로직과 동일
        _selectedMarkerId.value = mapContent.contentId
        Log.d(TAG, "🆔 [FLOW] selectedMarkerId 설정: ${mapContent.contentId}")
        
        _isLoading.value = true
        Log.d(TAG, "⏳ [FLOW] isLoading = true 설정")
        
        _isBottomSheetExpanded.value = true
        Log.d(TAG, "📈 [FLOW] isBottomSheetExpanded = true 설정")
        
        viewModelScope.launch {
            Log.d(TAG, "🚀 [FLOW] 코루틴 시작 - 데이터 로딩 시작")
            // 단일 마커의 경우 해당 마커의 contentId만 사용
            mapContentRepository.getContentsByIds(listOf(mapContent.contentId))
                .onSuccess { contents ->
                    Log.d(TAG, "✅ [FLOW] 데이터 로딩 성공: ${contents.size}개")
                    _bottomSheetContents.value = contents
                    _isLoading.value = false
                    Log.d(TAG, "📊 [FLOW] bottomSheetContents 업데이트 완료, isLoading = false")
                }
                .onFailure {
                    Log.e(TAG, "❌ [FLOW] 데이터 로딩 실패", it)
                    _bottomSheetContents.value = emptyList()
                    _isBottomSheetExpanded.value = false
                    _isLoading.value = false
                    Log.d(TAG, "🔄 [FLOW] 실패 처리 완료 - 상태 초기화")
                }
        }
        Log.d(TAG, "🔚 [FLOW] selectMarker() 완료")
    }

    // 클러스터 선택 처리
    fun selectCluster(clusterContents: List<MapContent>) {
        Log.e(TAG, "🎯🎯🎯 [FLOW] selectCluster() 시작: ${clusterContents.size}개 컨텐츠")
        Log.d(TAG, "🎯 [FLOW] selectCluster() 시작: ${clusterContents.size}개 컨텐츠")
        
        selectedMarker = null // 클러스터 선택 시에는 특정 마커 선택 없음
        Log.d(TAG, "📌 [FLOW] selectedMarker = null 설정")
        
        _selectedMarkerId.value = null
        Log.d(TAG, "🆔 [FLOW] selectedMarkerId = null 설정")
        
        _isLoading.value = true
        Log.d(TAG, "⏳ [FLOW] isLoading = true 설정")
        
        _isBottomSheetExpanded.value = true
        Log.d(TAG, "📈 [FLOW] isBottomSheetExpanded = true 설정")
        
        viewModelScope.launch {
            Log.d(TAG, "🚀 [FLOW] 코루틴 시작 - 클러스터 데이터 로딩 시작")
            val contentIds = clusterContents.map { it.contentId }
            Log.d(TAG, "📋 [FLOW] 로딩할 컨텐츠 ID들: $contentIds")
            
            mapContentRepository.getContentsByIds(contentIds)
                .onSuccess { contents ->
                    Log.d(TAG, "✅ [FLOW] 클러스터 데이터 로딩 성공: ${contents.size}개")
                    _bottomSheetContents.value = contents
                    _isLoading.value = false
                    Log.d(TAG, "📊 [FLOW] 클러스터 bottomSheetContents 업데이트 완료, isLoading = false")
                }
                .onFailure {
                    Log.e(TAG, "❌ [FLOW] 클러스터 데이터 로딩 실패", it)
                    _bottomSheetContents.value = emptyList()
                    _isBottomSheetExpanded.value = false
                    _isLoading.value = false
                    Log.d(TAG, "🔄 [FLOW] 클러스터 실패 처리 완료 - 상태 초기화")
                }
        }
        Log.d(TAG, "🔚 [FLOW] selectCluster() 완료")
    }

    fun clearSelectedMarker() {
        selectedMarker = null
        _selectedMarkerId.value = null
        _bottomSheetContents.value = emptyList()
        _isBottomSheetExpanded.value = false
        _isLoading.value = false
        Log.d(TAG, "마커 선택 해제됨")
    }

    fun isMarkerSelected(mapContent: MapContent): Boolean {
        return selectedMarker?.contentId == mapContent.contentId
    }

    // 지도 이동시 바텀시트 축소
    fun onMapMove() {
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "🗺️ [FLOW] onMapMove() 호출됨 - 시간: $currentTime")
        Log.d(TAG, "📊 [FLOW] 현재 isBottomSheetExpanded: ${_isBottomSheetExpanded.value}")
        Log.d(TAG, "📊 [FLOW] 현재 bottomSheetContents 크기: ${_bottomSheetContents.value.size}")
        Log.d(TAG, "📊 [FLOW] 현재 selectedMarkerId: ${_selectedMarkerId.value}")
        
        if (_isBottomSheetExpanded.value) {
            _isBottomSheetExpanded.value = false
            Log.d(TAG, "📉 [FLOW] 바텀시트 축소됨 - isBottomSheetExpanded = false")
        } else {
            Log.d(TAG, "ℹ️ [FLOW] 바텀시트가 이미 축소된 상태")
        }
    }

    // 바텀시트 확장 상태 업데이트 (UI에서 직접 드래그했을 때 사용)
    fun updateBottomSheetExpanded(isExpanded: Boolean) {
        Log.d(TAG, "🔄 [FLOW] updateBottomSheetExpanded() 호출: $isExpanded")
        _isBottomSheetExpanded.value = isExpanded
    }

    // 바텀시트 상태 변경
    fun onBottomSheetStateChange(expanded: Boolean) {
        _isBottomSheetExpanded.value = expanded
    }

    fun clearError() {
        errorMessage = null
    }

    // 바텀시트 닫기
    fun clearSelection() {
        _selectedMarkerId.value = null
        _bottomSheetContents.value = emptyList()
        _isBottomSheetExpanded.value = false
    }

    fun clusteringUpdated() {
        shouldUpdateClustering = false
    }

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        // 날짜가 변경되면 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng)
        }
    }

    fun toggleFilterTag(tagId: String) {
        selectedTags = if (selectedTags.contains(tagId)) {
            emptySet() // 이미 선택된 태그 클릭 시 선택 해제
        } else {
            setOf(tagId) // 새 태그 선택 시 기존 선택 해제하고 새 태그만 선택
        }

        // 선택된 태그에 따라 postType 업데이트
        selectedPostType = if (selectedTags.isEmpty()) {
            "ALL"
        } else {
            selectedTags.first() // 하나만 선택되므로 first() 사용
        }

        // 필터가 변경되면 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng)
        }
    }

    fun updatePostType(postType: String) {
        selectedPostType = postType

        // postType 변경 시 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng)
        }
    }

    fun clearAllFilters() {
        selectedTags = emptySet()
        selectedDate = LocalDate.now()
        selectedPostType = "ALL"

        // 필터 초기화 후 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng)
        }
    }


    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * 반경이 제공되지 않은 경우 사용할 기본 반경 (이전 버전 호환성)
     */
    private fun getDefaultRadius(): Int {
        return 2000 // 기본 2km
    }
    
    /**
     * 화면 영역 기반으로 맵 데이터를 로드하는 새로운 함수
     * @param latitude 중심점 위도
     * @param longitude 중심점 경도  
     * @param radius 화면 영역 기반 계산된 반경
     */
    fun loadMapContentsWithCalculatedRadius(
        latitude: Double,
        longitude: Double,
        radius: Int
    ) {
        Log.d(TAG, "🎯 화면 영역 기반 데이터 로드 시작 - 반경: ${radius}m")
        loadMapContents(latitude, longitude, radius)
    }

    // 툴팁 관리 함수들
    fun showTooltip(content: MapContent, naverMap: NaverMap, type: TooltipType) {
        val latLng = com.naver.maps.geometry.LatLng(content.location.latitude, content.location.longitude)
        val screenPoint = naverMap.projection.toScreenLocation(latLng)
        val position = Offset(screenPoint.x.toFloat(), screenPoint.y.toFloat())
        
        Log.d(TAG, "🎯 showTooltip 호출됨: ${content.title}")
        Log.d(TAG, "📍 마커 위치: lat=${content.location.latitude}, lng=${content.location.longitude}")
        Log.d(TAG, "📱 화면 좌표: x=${screenPoint.x}, y=${screenPoint.y}")
        Log.d(TAG, "🎨 툴팁 타입: $type")
        
        _tooltipState.value = TooltipState(
            isVisible = true,
            content = content,
            position = position,
            type = type
        )
        
        Log.d(TAG, "✅ 툴팁 상태 업데이트 완료: ${_tooltipState.value}")
    }

    fun hideTooltip() {
        Log.d(TAG, "🫥 hideTooltip 호출됨")
        _tooltipState.value = _tooltipState.value.copy(isVisible = false)
        Log.d(TAG, "❌ 툴팁 숨김 완료")
    }

    fun updateTooltipPosition(newPosition: Offset) {
        if (_tooltipState.value.isVisible) {
            // Log.d(TAG, "📍 툴팁 위치 업데이트: $newPosition") // 너무 많이 호출되서 주석
            _tooltipState.value = _tooltipState.value.copy(position = newPosition)
        }
    }
    
    // 위치 공유 관련 함수들을 LocationSharingManager로 위임
    fun addSharedLocation(
        userName: String,
        latitude: Double,
        longitude: Double,
        displayUntilString: String,
        shareId: String
    ) {
        locationSharingManager.addSharedLocation(userName, latitude, longitude, displayUntilString, shareId)
    }
    
    fun removeSharedLocation(shareId: String) {
        locationSharingManager.removeSharedLocation(shareId)
    }
    
    fun cleanupExpiredLocations() {
        locationSharingManager.cleanupExpiredLocations()
    }
}