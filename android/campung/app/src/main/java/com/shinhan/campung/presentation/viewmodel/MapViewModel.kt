package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.repository.MapContentRepository
import com.shinhan.campung.data.remote.response.MapContent
import com.shinhan.campung.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapContentRepository: MapContentRepository
) : BaseViewModel() {

    // UI States
    private val _bottomSheetContents = MutableStateFlow<List<MapContent>>(emptyList())
    val bottomSheetContents: StateFlow<List<MapContent>> = _bottomSheetContents.asStateFlow()

    private val _selectedMarkerId = MutableStateFlow<Long?>(null)
    val selectedMarkerId: StateFlow<Long?> = _selectedMarkerId.asStateFlow()

    private val _isBottomSheetExpanded = MutableStateFlow(false)
    val isBottomSheetExpanded: StateFlow<Boolean> = _isBottomSheetExpanded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    var isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    fun loadMapContents(
        latitude: Double,
        longitude: Double,
        radius: Int? = null,
        postType: String? = null
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

        // 이전 요청과 비교해서 중복 요청 방지
        lastRequestParams?.let { lastParams ->
            val locationDistance = calculateDistance(
                lastParams.location.first, lastParams.location.second,
                latitude, longitude
            )

            // 위치는 같고 (500m 이내), 다른 파라미터도 동일하면 스킵
            if (locationDistance < 500.0 &&
                lastParams.date == currentParams.date &&
                lastParams.tags == currentParams.tags &&
                lastParams.postType == currentParams.postType) {
                return
            }
        }

        // 500ms 디바운스 적용
        debounceJob = viewModelScope.launch {
            delay(500)

            isLoading = true
            errorMessage = null
            lastRequestLocation = currentLocation
            lastRequestParams = currentParams

            try {
                // 선택된 날짜를 문자열로 변환
                val dateString = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val response = mapRepository.getMapContents(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    postType = postType ?: selectedPostType,
                    date = dateString
                ).getOrThrow()

                if (response.success) {
                    mapContents = response.data.contents
                    shouldUpdateClustering = true

                    // 새로운 데이터 로드 시 선택된 마커가 여전히 존재하는지 확인
                    selectedMarker?.let { selected ->
                        val stillExists = mapContents.any { it.contentId == selected.contentId }
                        if (!stillExists) {
                            selectedMarker = null // 더 이상 존재하지 않으면 선택 해제
                        }
                    }
                } else {
                    errorMessage = response.message
                }

            _isLoading.value = false
            } catch (throwable: Throwable) {
                errorMessage = throwable.message ?: "알 수 없는 오류가 발생했습니다"
            }

            isLoading = false
        }
    }

    // 마커 선택 상태 관리 함수들
    fun selectMarker(mapContent: MapContent) {
        selectedMarker = mapContent
        Log.d(TAG, "마커 선택됨: ${mapContent.title}")
    }

    fun clearSelectedMarker() {
        selectedMarker = null
        Log.d(TAG, "마커 선택 해제됨")
    }

    fun isMarkerSelected(mapContent: MapContent): Boolean {
        return selectedMarker?.contentId == mapContent.contentId

    // 지도 이동시 바텀시트 축소
    fun onMapMove() {
        if (_isBottomSheetExpanded.value) {
            _isBottomSheetExpanded.value = false
        }
    }

    // 바텀시트 상태 변경
    fun onBottomSheetStateChange(expanded: Boolean) {
        _isBottomSheetExpanded.value = expanded

    fun clearError() {
        errorMessage = null
    }

    // 바텀시트 닫기
    fun clearSelection() {
        _selectedMarkerId.value = null
        _bottomSheetContents.value = emptyList()
        _isBottomSheetExpanded.value = false

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
}