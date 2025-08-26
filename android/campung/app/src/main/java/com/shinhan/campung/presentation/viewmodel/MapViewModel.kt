package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.remote.response.MapContent
import com.shinhan.campung.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "MapViewModel"
    }

    var mapContents by mutableStateOf<List<MapContent>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var shouldUpdateClustering by mutableStateOf(false)
        private set

    // 필터 상태
    var selectedDate by mutableStateOf(LocalDate.now())
        private set
    
    var selectedTags by mutableStateOf<Set<String>>(emptySet())
        private set

    private var debounceJob: Job? = null
    private var lastRequestLocation: Pair<Double, Double>? = null
    private var lastRequestParams: RequestParams? = null
    
    private data class RequestParams(
        val location: Pair<Double, Double>,
        val date: LocalDate,
        val tags: Set<String>,
        val postType: String?
    )

    fun loadMapContents(
        latitude: Double,
        longitude: Double,
        radius: Int? = null,
        postType: String? = "ALL"
    ) {
        // 이전 요청 취소
        debounceJob?.cancel()
        
        val currentLocation = Pair(latitude, longitude)
        val currentParams = RequestParams(
            location = currentLocation,
            date = selectedDate,
            tags = selectedTags,
            postType = postType
        )
        
        // 이전 요청과 비교해서 중복 요청 방지
        lastRequestParams?.let { lastParams ->
            val locationDistance = calculateDistance(
                lastParams.location.first, lastParams.location.second,
                latitude, longitude
            )
            
            // 위치는 같고(500m 이내), 다른 파라미터도 동일하면 스킵
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
                    postType = postType,
                    date = dateString
                ).getOrThrow()
                
                if (response.success) {
                    mapContents = response.data.contents
                    shouldUpdateClustering = true
                } else {
                    errorMessage = response.message
                }
            } catch (throwable: Throwable) {
                errorMessage = throwable.message ?: "알 수 없는 오류가 발생했습니다"
            }

            isLoading = false
        }
    }

    fun clearError() {
        errorMessage = null
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
            selectedTags - tagId
        } else {
            selectedTags + tagId
        }
        
        // 필터가 변경되면 다시 로드
        lastRequestLocation?.let { (lat, lng) ->
            loadMapContents(lat, lng)
        }
    }
    
    fun clearAllFilters() {
        selectedTags = emptySet()
        selectedDate = LocalDate.now()
        
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