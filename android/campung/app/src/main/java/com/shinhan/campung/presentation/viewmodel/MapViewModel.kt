package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naver.maps.map.NaverMap
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.model.MapRecord
import com.shinhan.campung.data.model.MapItem
import com.shinhan.campung.data.model.MapContentItem
import com.shinhan.campung.data.model.MapRecordItem
import com.shinhan.campung.data.model.createMixedMapItems
import com.shinhan.campung.data.repository.MapContentRepository
import com.shinhan.campung.data.repository.MapRepository
import com.shinhan.campung.data.repository.POIRepository
import com.shinhan.campung.data.repository.RecordingRepository
import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.mapper.ContentMapper
import com.shinhan.campung.data.model.POIData
import com.shinhan.campung.data.model.ContentCategory
import com.shinhan.campung.presentation.ui.components.TooltipState
import com.shinhan.campung.presentation.ui.components.TooltipType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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
    private val poiRepository: POIRepository,
    private val recordingRepository: RecordingRepository,
    private val authDataStore: AuthDataStore,
    private val contentMapper: ContentMapper,
    val locationSharingManager: LocationSharingManager // public으로 노출
) : BaseViewModel() {
    fun getLastKnownLocation(): Pair<Double, Double>? = lastRequestLocation

    // 현재 사용자 ID
    val currentUserId: StateFlow<String?> = authDataStore.userIdFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    // UI States
    private val _bottomSheetContents = MutableStateFlow<List<MapContent>>(emptyList())
    val bottomSheetContents: StateFlow<List<MapContent>> = _bottomSheetContents.asStateFlow()
    
    // 통합 바텀시트 아이템 (MapItem 사용)
    private val _bottomSheetItems = MutableStateFlow<List<MapItem>>(emptyList())
    val bottomSheetItems: StateFlow<List<MapItem>> = _bottomSheetItems.asStateFlow()

    private val _selectedMarkerId = MutableStateFlow<Long?>(null)
    val selectedMarkerId: StateFlow<Long?> = _selectedMarkerId.asStateFlow()

    private val _isBottomSheetExpanded = MutableStateFlow(false)
    val isBottomSheetExpanded: StateFlow<Boolean> = _isBottomSheetExpanded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 툴팁 상태 관리
    private val _tooltipState = MutableStateFlow(TooltipState())
    val tooltipState: StateFlow<TooltipState> = _tooltipState.asStateFlow()

    // 오디오 플레이어 상태 관리
    private val _currentPlayingRecord = MutableStateFlow<MapRecord?>(null)
    val currentPlayingRecord: StateFlow<MapRecord?> = _currentPlayingRecord.asStateFlow()

    // 위치 공유 상태를 LocationSharingManager에서 가져옴
    val sharedLocations: StateFlow<List<com.shinhan.campung.data.model.SharedLocation>> =
        locationSharingManager.sharedLocations

    // POI 관련 상태
    private val _poiData = MutableStateFlow<List<POIData>>(emptyList())
    val poiData: StateFlow<List<POIData>> = _poiData.asStateFlow()

    private val _isPOIVisible = MutableStateFlow(true) // 테스트를 위해 기본값을 true로 변경
    val isPOIVisible: StateFlow<Boolean> = _isPOIVisible.asStateFlow()

    private val _selectedPOICategory = MutableStateFlow<String?>(null)
    val selectedPOICategory: StateFlow<String?> = _selectedPOICategory.asStateFlow()

    private val _isPOILoading = MutableStateFlow(false)
    val isPOILoading: StateFlow<Boolean> = _isPOILoading.asStateFlow()

    private val _selectedPOI = MutableStateFlow<POIData?>(null)
    val selectedPOI: StateFlow<POIData?> = _selectedPOI.asStateFlow()

    private val _showPOIDialog = MutableStateFlow(false)
    val showPOIDialog: StateFlow<Boolean> = _showPOIDialog.asStateFlow()

    private val _isLoadingPOIDetail = MutableStateFlow(false)
    val isLoadingPOIDetail: StateFlow<Boolean> = _isLoadingPOIDetail.asStateFlow()

    // MapViewModel.kt - 상단 필드들 옆에 추가
    private val _serverWeather = MutableStateFlow<String?>(null)
    val serverWeather: StateFlow<String?> = _serverWeather

    private val _serverTemperature = MutableStateFlow<Int?>(null)
    val serverTemperature: StateFlow<Int?> = _serverTemperature

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

    var mapRecords by mutableStateOf<List<MapRecord>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var shouldUpdateClustering by mutableStateOf(false)
        private set

    // 선택된 마커 상태 추가
    var selectedMarker by mutableStateOf<MapContent?>(null)
        private set

    // 선택된 Record 상태 추가
    var selectedRecord by mutableStateOf<MapRecord?>(null)
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
        Log.d(TAG, "🎯 하이라이트 요청 등록: $contentId")
        pendingHighlightId = contentId

        // 이미 로드된 데이터에서 해당 마커를 찾아서 즉시 하이라이트
        mapContents.firstOrNull { it.contentId == contentId }?.let { content ->
            Log.d(TAG, "✅ 기존 데이터에서 마커 발견 - 즉시 선택: ${content.title}")
            selectMarker(content)
            pendingHighlightId = null // 처리 완료
        } ?: Log.d(TAG, "⏳ 기존 데이터에 없음 - 다음 로드 시 처리 예약")
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

        // ✅ 스마트 중복 요청 스킵 로직
        if (!force && !_isLoading.value) { // 로딩 중이 아닐 때만 중복 체크
            lastRequestParams?.let { lastParams ->
                val locationDistance = calculateDistance(
                    lastParams.location.first, lastParams.location.second,
                    latitude, longitude
                )
                
                // 반경 기반 임계값 계산 (작은 반경일수록 더 민감하게)
                val threshold = when {
                    (radius ?: getDefaultRadius()) < 500 -> 25.0    // 500m 미만: 25m 임계값
                    (radius ?: getDefaultRadius()) < 1500 -> 75.0   // 1.5km 미만: 75m 임계값
                    else -> 150.0  // 1.5km 이상: 150m 임계값
                }

                if (locationDistance < threshold &&
                    lastParams.date == currentParams.date &&
                    lastParams.tags == currentParams.tags &&
                    lastParams.postType == currentParams.postType) {
                    Log.d(TAG, "스마트 중복 요청 스킵 - 거리: ${locationDistance.toInt()}m < 임계값: ${threshold.toInt()}m")
                    return
                }
            }
        } else if (_isLoading.value) {
            Log.d(TAG, "이미 로딩 중 - 새 요청 무시")
            return
        } else {
            Log.d(TAG, "강제 로드 모드 - 중복 체크 무시")
        }

        // 적응형 디바운스 적용 (강제 로드시 더 빠르게)
        val debounceDelay = if (force) 50L else 100L
        debounceJob = viewModelScope.launch {
            delay(debounceDelay)

            Log.d(TAG, "🚀 데이터 로드 시작 - 위치: (${latitude}, ${longitude}), 반경: ${radius ?: getDefaultRadius()}m")

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
                    val rawContents = response.data.contents
                    val newRecords = response.data.records ?: emptyList() // null일 경우 빈 리스트
                    
                    // ContentData를 MapContent로 변환
                    val newContents = rawContents.map { contentData ->
                        contentMapper.toMapContent(contentData)
                    }

                    Log.d(TAG, "✅ 데이터 로드 성공: ${newContents.size}개 Content 마커, ${newRecords.size}개 Record 마커")

                    // 데이터 업데이트 및 즉시 클러스터링 트리거
                    mapContents = newContents
                    mapRecords = newRecords
                    shouldUpdateClustering = true

                    // 로딩 상태는 클러스터링 완료 후 해제하도록 변경
                    // _isLoading.value = false

                    // ✅ 방금 등록한 ID가 있으면 자동으로 선택/하이라이트
                    pendingHighlightId?.let { id ->
                        Log.d(TAG, "🎯 pendingHighlightId 처리 시작: $id")
                        Log.d(TAG, "📋 로드된 컨텐츠 IDs: ${newContents.map { it.contentId }}")

                        newContents.firstOrNull { it.contentId == id }?.let { content ->
                            Log.d(TAG, "✅ 하이라이트 대상 마커 찾음: ${content.title} (${content.contentId})")

                            // 클러스터링 완료 후 마커 선택
                            selectMarker(content)

                        } ?: Log.w(TAG, "⚠️ 하이라이트 대상 마커를 찾지 못함: $id")

                        pendingHighlightId = null
                    }

                    // 선택된 마커가 새 데이터에 없으면 해제
                    val rawWeather = response.data.emotionWeather
                    val rawTemp = response.data.emotionTemperature

                    Log.d("MapViewModel", "🌤️ 서버 원본 데이터 - rawWeather: '$rawWeather', rawTemp: $rawTemp")

                    // 서버에서 날씨 데이터가 없다면 임시 테스트 데이터 사용
                    val testWeather = if (rawWeather.isNullOrBlank()) "맑음" else rawWeather
                    val testTemp = rawTemp ?: 25.0

                    Log.d("MapViewModel", "🧪 테스트 데이터 적용 - testWeather: '$testWeather', testTemp: $testTemp")

                    _serverWeather.value = normalizeWeather(testWeather)
                    _serverTemperature.value = kotlin.math.round(testTemp).toInt()

                    Log.d("MapViewModel", "🎯 최종 변환된 데이터 - serverWeather: '${_serverWeather.value}', serverTemperature: ${_serverTemperature.value}")

                    // 새로운 데이터 로드 시 선택된 마커가 여전히 존재하는지 확인
                    selectedMarker?.let { selected ->
                        val stillExists = mapContents.any { it.contentId == selected.contentId }
                        if (!stillExists) {
                            Log.d(TAG, "⚠️ 기존 선택 마커가 새 데이터에 없음 - 선택 해제")
                            selectedMarker = null
                        }
                    }
                } else {
                    errorMessage = response.message
                    _isLoading.value = false
                }

            } catch (t: Throwable) {
                Log.e(TAG, "❌ 데이터 로드 예외", t)
                errorMessage = t.message ?: "알 수 없는 오류가 발생했습니다"
                _isLoading.value = false
            }
        }
    }

    // 마커 선택 상태 관리 함수들
    fun selectMarker(mapContent: MapContent) {
        selectedMarker = mapContent
        
        // 바텀시트 데이터 로딩
        _selectedMarkerId.value = mapContent.contentId
        _isLoading.value = true
        _isBottomSheetExpanded.value = true
        
        // 통합 바텀시트 아이템 초기화 (클러스터 선택 후 개별 마커 선택 시 중복 방지)
        _bottomSheetItems.value = emptyList()
        
        viewModelScope.launch {
            try {
                val result = mapContentRepository.getContentsByIds(listOf(mapContent.contentId))
                
                result
                    .onSuccess { contents ->
                        if (contents.isNotEmpty()) {
                            _bottomSheetContents.value = contents
                            _isLoading.value = false
                        } else {
                            handleEmptyDataFallback(mapContent)
                        }
                    }
                    .onFailure { exception ->
                        Log.e(TAG, "API 호출 실패: ${exception.message}")
                        handleApiFailureFallback(mapContent, exception)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "selectMarker 예외 발생: ${e.message}")
                handleApiFailureFallback(mapContent, e)
            }
        }
    }

    // 클러스터 선택 처리
    fun selectCluster(clusterContents: List<MapContent>) {
        selectedMarker = null
        _selectedMarkerId.value = null
        _isLoading.value = true
        _isBottomSheetExpanded.value = true
        
        // 통합 바텀시트 아이템 초기화 (혼합 클러스터 선택 후 기존 클러스터 선택 시 중복 방지)
        _bottomSheetItems.value = emptyList()
        
        viewModelScope.launch {
            val contentIds = clusterContents.map { it.contentId }
            
            mapContentRepository.getContentsByIds(contentIds)
                .onSuccess { contents ->
                    _bottomSheetContents.value = contents
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    Log.e(TAG, "클러스터 데이터 로딩 실패", exception)
                    _bottomSheetContents.value = emptyList()
                    _isBottomSheetExpanded.value = false
                    _isLoading.value = false
                }
        }
    }

    // 통합 클러스터 선택 처리 (MapItem 사용)
    fun selectMixedCluster(clusterItems: List<MapItem>) {
        Log.d(TAG, "🎯 [MIXED] selectMixedCluster 호출: ${clusterItems.size}개 아이템")
        
        selectedMarker = null
        _selectedMarkerId.value = null
        _isLoading.value = true
        _isBottomSheetExpanded.value = true
        
        // 기존 바텀시트 콘텐츠 초기화 (개별 마커 선택 후 클러스터 선택 시 중복 방지)
        _bottomSheetContents.value = emptyList()
        
        viewModelScope.launch {
            try {
                // Content와 Record 분리
                val contentItems = clusterItems.filterIsInstance<MapContentItem>()
                val recordItems = clusterItems.filterIsInstance<MapRecordItem>()
                
                Log.d(TAG, "🔍 [MIXED] Content 아이템: ${contentItems.size}개, Record 아이템: ${recordItems.size}개")
                
                // Content는 서버에서 상세 정보를 가져와야 함 (기존 로직 유지)
                val detailedContents = if (contentItems.isNotEmpty()) {
                    val contentIds = contentItems.map { it.content.contentId }
                    mapContentRepository.getContentsByIds(contentIds).getOrElse { 
                        Log.e(TAG, "Content 상세 정보 로딩 실패")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                // Record는 이미 전체 정보를 가지고 있음
                val records = recordItems.map { it.record }
                
                // 통합 MapItem 리스트 생성 (createdAt 기준으로 정렬)
                val mixedItems = createMixedMapItems(detailedContents, records)
                
                Log.d(TAG, "✅ [MIXED] 통합 아이템 생성 완료: ${mixedItems.size}개")
                
                // 기존 bottomSheetContents도 업데이트 (하위 호환성)
                _bottomSheetContents.value = detailedContents
                
                // 새로운 통합 아이템 설정
                _bottomSheetItems.value = mixedItems
                
                _isLoading.value = false
                
            } catch (exception: Exception) {
                Log.e(TAG, "통합 클러스터 데이터 로딩 실패", exception)
                _bottomSheetContents.value = emptyList()
                _bottomSheetItems.value = emptyList()
                _isBottomSheetExpanded.value = false
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedMarker() {
        selectedMarker = null
        _selectedMarkerId.value = null
        _isLoading.value = false
        
        // 모든 바텀시트 데이터 초기화 (중복 표시 방지)
        _bottomSheetContents.value = emptyList()
        _bottomSheetItems.value = emptyList()
        _isBottomSheetExpanded.value = false
    }

    fun loadHotContents() {
        Log.d(TAG, "🔥 [FLOW] loadHotContents 시작 - 핫 게시글 로드")
        _isLoading.value = true

        viewModelScope.launch {
            try {
                mapContentRepository.getHotContents()
                    .onSuccess { hotContents ->
                        Log.d(TAG, "✅ [FLOW] 핫 콘텐츠 로드 성공 - ${hotContents.size}개")
                        _bottomSheetContents.value = hotContents
                        _isBottomSheetExpanded.value = true // 핫 콘텐츠 표시 시 확장
                        _isLoading.value = false
                    }
                    .onFailure { e ->
                        Log.e(TAG, "❌ [FLOW] 핫 콘텐츠 로드 실패", e)
                        _bottomSheetContents.value = emptyList()
                        _isBottomSheetExpanded.value = false
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [FLOW] 핫 콘텐츠 로드 중 예외", e)
                _bottomSheetContents.value = emptyList()
                _isBottomSheetExpanded.value = false
                _isLoading.value = false
            }
        }
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
        _isBottomSheetExpanded.value = false

        // 핫 콘텐츠로 복귀
        loadHotContents()
    }

    fun clusteringUpdated() {
        shouldUpdateClustering = false
    }

    fun updateSelectedDate(date: LocalDate) {
        selectedDate = date

        // 기존 마커들 즉시 클리어
        mapContents = emptyList()
        mapRecords = emptyList()
        shouldUpdateClustering = true

        // 선택된 마커도 클리어
        selectedMarker = null
        _selectedMarkerId.value = null

        // lastRequestParams 초기화로 새로운 요청 허용
        lastRequestParams = null

        // 핫 콘텐츠로 복귀
        loadHotContents()
    }

    fun selectPreviousDate() {
        val previousDate = selectedDate.minusDays(1)
        updateSelectedDate(previousDate)
    }

    fun selectNextDate() {
        val nextDate = selectedDate.plusDays(1)
        val today = LocalDate.now()

        // 오늘 날짜보다 미래로는 갈 수 없도록 제한
        if (nextDate.isBefore(today) || nextDate.isEqual(today)) {
            updateSelectedDate(nextDate)
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

        // 기존 마커들 즉시 클리어
        mapContents = emptyList()
        mapRecords = emptyList()
        shouldUpdateClustering = true

        // 선택된 마커도 클리어
        selectedMarker = null
        _selectedMarkerId.value = null

        // lastRequestParams 초기화로 새로운 요청 허용
        lastRequestParams = null

        // 필터가 변경되면 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng, force = true)
        }

        // 마커 데이터가 없을 때 핫 콘텐츠로 복귀
        if (mapContents.isEmpty()) {
            loadHotContents()
        }
    }

    fun updatePostType(postType: String) {
        selectedPostType = postType

        // 기존 마커들 즉시 클리어
        mapContents = emptyList()
        mapRecords = emptyList()
        shouldUpdateClustering = true

        // 선택된 마커도 클리어
        selectedMarker = null
        _selectedMarkerId.value = null

        // lastRequestParams 초기화로 새로운 요청 허용
        lastRequestParams = null

        // postType 변경 시 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng, force = true)
        }

        // 마커 데이터가 없을 때 핫 콘텐츠로 복귀
        if (mapContents.isEmpty()) {
            loadHotContents()
        }
    }

    fun clearAllFilters() {
        selectedTags = emptySet()
        selectedDate = LocalDate.now()
        selectedPostType = "ALL"

        // 기존 마커들 즉시 클리어
        mapContents = emptyList()
        mapRecords = emptyList()
        shouldUpdateClustering = true

        // 선택된 마커도 클리어
        selectedMarker = null
        clearSelectedMarker()

        // lastRequestParams 초기화로 새로운 요청 허용
        lastRequestParams = null

        // 필터 초기화 후 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng, force = true)
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
    }// MapViewModel.kt (파일 아무 하단 유틸 영역)
    private fun normalizeWeather(raw: String?): String? {
        val k = raw?.trim()?.lowercase() ?: return null
        return when (k) {
            "맑음", "해", "쾌청", "sun", "fine", "clear" -> "sunny"
            "구름", "흐림", "흐림많음", "cloud", "overcast", "cloudy", "clouds" -> "clouds"
            "비", "소나기", "drizzle", "rain shower", "rainy", "rain" -> "rain"
            "천둥", "천둥번개", "번개", "뇌우", "thunder", "storm", "thunderstorm", "stormy" -> "thunderstorm"
            else -> null
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

    // 오디오 플레이어 관련 함수들
    fun playRecord(record: MapRecord) {
        Log.d(TAG, "🎵 Record 재생 시작: ${record.recordUrl}")

        // Content 마커 선택 해제
        selectedMarker = null

        // Record 선택 상태 업데이트
        selectedRecord = record
        _currentPlayingRecord.value = record
    }

    fun stopRecord() {
        Log.d(TAG, "⏹️ Record 재생 중지")

        // Record 선택 해제
        selectedRecord = null
        _currentPlayingRecord.value = null
    }

    fun deleteRecord(recordId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Record 삭제 시작: $recordId")
                
                recordingRepository.deleteRecord(recordId)
                
                // 현재 재생 중인 record가 삭제된 것이면 정지
                if (selectedRecord?.recordId == recordId) {
                    stopRecord()
                }
                
                // 지도에서 해당 record 제거 (새로운 리스트로 교체)
                mapRecords = mapRecords.filter { it.recordId != recordId }
                
                // 클러스터링 업데이트 트리거 (토글 방식으로 확실히 업데이트)
                shouldUpdateClustering = !shouldUpdateClustering
                
                Log.d(TAG, "✅ Record 삭제 완료: $recordId")
                onSuccess()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Record 삭제 실패: $recordId", e)
                onError(e.message ?: "삭제 중 오류가 발생했습니다")
            }
        }
    }

    fun isRecordSelected(record: MapRecord): Boolean {
        return selectedRecord?.recordId == record.recordId
    }

    // 클러스터링 완료 콜백
    fun onClusteringCompleted() {
        shouldUpdateClustering = false
        _isLoading.value = false // 클러스터링 완료 후 로딩 상태 해제
        Log.d(TAG, "🎯 클러스터링 완료 - 로딩 상태 해제")
    }

    fun isMyRecord(record: MapRecord, currentUserId: String?): Boolean {
        return currentUserId != null && record.userId == currentUserId
    }

    // ===== POI 관련 함수들 =====

    /**
     * POI 표시 상태 토글
     */
    fun togglePOIVisibility() {
        _isPOIVisible.value = !_isPOIVisible.value
        Log.d(TAG, "🏪 POI 표시 상태 토글: ${_isPOIVisible.value}")

        if (_isPOIVisible.value) {
            // POI가 켜질 때 현재 위치 기반으로 로드
            lastRequestLocation?.let { (lat, lng) ->
                Log.d(TAG, "🏪 POI 토글 ON - 현재 위치로 데이터 로드: ($lat, $lng)")
                loadPOIData(lat, lng)
            } ?: Log.w(TAG, "🏪 POI 토글 ON - 현재 위치 정보 없음")
        } else {
            // POI가 꺼질 때 데이터 클리어
            Log.d(TAG, "🏪 POI 토글 OFF - 데이터 클리어")
            _poiData.value = emptyList()
        }
    }

    /**
     * POI 카테고리 선택
     */
    fun selectPOICategory(category: String?) {
        _selectedPOICategory.value = category
        Log.d(TAG, "🏪 POI 카테고리 선택: $category")

        if (_isPOIVisible.value) {
            lastRequestLocation?.let { (lat, lng) ->
                Log.d(TAG, "🏪 카테고리 변경으로 POI 데이터 재로드: ($lat, $lng), 카테고리=$category")
                loadPOIData(lat, lng, category)
            } ?: Log.w(TAG, "🏪 카테고리 선택 - 현재 위치 정보 없음")
        } else {
            Log.d(TAG, "🏪 POI가 비활성화 상태 - 카테고리 선택만 저장")
        }
    }

    /**
     * 중심점과 반경 기반으로 POI 데이터 로드
     */
    fun loadPOIData(
        latitude: Double,
        longitude: Double,
        category: String? = _selectedPOICategory.value,
        radius: Int = 1000
    ) {
        if (!_isPOIVisible.value) {
            Log.d(TAG, "🏪 POI가 비활성화 상태 - 데이터 로드 스킵")
            return
        }

        viewModelScope.launch {
            _isPOILoading.value = true
            Log.d(TAG, "🏪 POI 데이터 로드 시작: 위치=($latitude, $longitude), 카테고리=$category, 반경=${radius}m")

            try {
                val result = poiRepository.getNearbyPOIs(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    category = category
                )

                result.onSuccess { pois ->
                    val validPois = pois.filter { it.thumbnailUrl != null }
                    _poiData.value = validPois
                    Log.d(TAG, "🏪 POI 데이터 로드 성공: 전체 ${pois.size}개, 유효(썸네일 있음) ${validPois.size}개")

                    validPois.forEachIndexed { index, poi ->
                        Log.v(TAG, "🏪 POI[$index]: ${poi.name} (${poi.category}) - ${poi.thumbnailUrl}")
                        Log.v(TAG, "🏪 POI[$index] Summary: ${poi.currentSummary}")
                    }
                }.onFailure { throwable ->
                    Log.e(TAG, "🏪 POI 데이터 로드 실패 - 테스트 더미 데이터 사용", throwable)

                    // 테스트용 더미 POI 데이터
                    val dummyPois = listOf(
                        POIData(
                            id = 1L,
                            name = "테스트 카페",
                            category = "cafe",
                            address = "서울시 강남구",
                            latitude = latitude + 0.001,
                            longitude = longitude + 0.001,
                            thumbnailUrl = "https://picsum.photos/200/200?random=1",
                            currentSummary = "아늑한 분위기의 카페입니다. 신선한 원두로 내린 커피와 다양한 디저트를 즐길 수 있어요."
                        ),
                        POIData(
                            id = 2L,
                            name = "테스트 음식점",
                            category = "restaurant",
                            address = "서울시 서초구",
                            latitude = latitude - 0.001,
                            longitude = longitude - 0.001,
                            thumbnailUrl = "https://picsum.photos/200/200?random=2",
                            currentSummary = "맛있는 한식을 제공하는 음식점입니다. 집밥 같은 따뜻한 음식과 정성스러운 서비스가 특징이에요."
                        )
                    )
                    _poiData.value = dummyPois
                    Log.d(TAG, "🏪 테스트 더미 POI ${dummyPois.size}개 로드됨")
                }

            } catch (e: Exception) {
                Log.e(TAG, "🏪 POI 데이터 로드 예외", e)
                _poiData.value = emptyList()
            } finally {
                _isPOILoading.value = false
                Log.d(TAG, "🏪 POI 로딩 상태 종료")
            }
        }
    }

    /**
     * POI 클릭 처리
     */
    fun onPOIClick(poi: POIData) {
        Log.d(TAG, "🏪 POI 클릭: ${poi.name} (${poi.category}) at (${poi.latitude}, ${poi.longitude})")
        Log.d(TAG, "🏪 POI 정보 - 주소: ${poi.address}, 전화: ${poi.phone}, 평점: ${poi.rating}")

        _selectedPOI.value = poi
        _showPOIDialog.value = true

        // 상세 정보 및 요약 조회
        loadPOIDetail(poi.id)

        Log.d(TAG, "🏪 POI 다이얼로그 표시")
    }

    /**
     * POI 다이얼로그 닫기
     */
    fun dismissPOIDialog() {
        _showPOIDialog.value = false
        _selectedPOI.value = null
        _isLoadingPOIDetail.value = false
        Log.d(TAG, "🏪 POI 다이얼로그 닫힘")
    }

    /**
     * POI 상세 정보 조회 (요약 포함)
     */
    fun loadPOIDetail(landmarkId: Long) {
        viewModelScope.launch {
            _isLoadingPOIDetail.value = true

            try {
                poiRepository.getLandmarkDetail(
                    landmarkId = landmarkId
                ).onSuccess { detailedPOI ->
                    // 현재 선택된 POI를 상세 정보로 업데이트
                    _selectedPOI.value = detailedPOI
                    Log.d(TAG, "🏪 POI 상세 정보 로드 성공: ${detailedPOI.name}, 요약: ${detailedPOI.currentSummary?.take(100)}...")
                }.onFailure { error ->
                    Log.e(TAG, "🏪 POI 상세 정보 로드 실패: ${error.message}", error)
                    // 실패해도 기존 POI 정보는 유지
                }
            } catch (e: Exception) {
                Log.e(TAG, "🏪 POI 상세 정보 로드 중 예외 발생: ${e.message}", e)
            } finally {
                _isLoadingPOIDetail.value = false
            }
        }
    }

    /**
     * 화면 이동 시 POI 데이터 업데이트
     */
    fun updatePOIForLocation(latitude: Double, longitude: Double, radius: Int) {
        if (_isPOIVisible.value) {
            Log.d(TAG, "🏪 화면 이동으로 POI 업데이트: ($latitude, $longitude), 반경=${radius}m")
            loadPOIData(latitude, longitude, radius = radius)
        } else {
            Log.v(TAG, "🏪 POI 비활성화 상태 - 화면 이동 업데이트 스킵")
        }
    }
    
    // ===== 폴백 처리 함수들 =====
    
    /**
     * API는 성공했지만 빈 데이터를 받았을 때의 폴백 처리
     */
    private fun handleEmptyDataFallback(mapContent: MapContent) {
        try {
            // 현재 mapContents에서 해당 content 찾기
            val localContent = mapContents.find { it.contentId == mapContent.contentId }
            
            if (localContent != null) {
                _bottomSheetContents.value = listOf(localContent)
                _isLoading.value = false
            } else {
                // 로컬에도 없으면 전달받은 mapContent 자체를 사용
                _bottomSheetContents.value = listOf(mapContent)
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "빈 데이터 폴백 처리 중 예외", e)
            handleFallbackFailure()
        }
    }
    
    /**
     * API 호출이 실패했을 때의 폴백 처리
     */
    private fun handleApiFailureFallback(mapContent: MapContent, exception: Throwable) {
        try {
            // 네트워크 오류나 서버 오류 시에도 로컬 데이터 사용 시도
            val localContent = mapContents.find { it.contentId == mapContent.contentId }
            
            if (localContent != null) {
                _bottomSheetContents.value = listOf(localContent)
                _isLoading.value = false
            } else {
                _bottomSheetContents.value = listOf(mapContent)
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "API 실패 폴백 처리 중 예외", e)
            handleFallbackFailure()
        }
    }
    
    /**
     * 모든 폴백이 실패했을 때의 최후 처리
     */
    private fun handleFallbackFailure() {
        _bottomSheetContents.value = emptyList()
        _isBottomSheetExpanded.value = false
        _isLoading.value = false
        
        // 선택된 마커 상태도 해제
        selectedMarker = null
        _selectedMarkerId.value = null
    }
}