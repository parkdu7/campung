package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.remote.response.MapContent
import com.shinhan.campung.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : BaseViewModel() {

    var mapContents by mutableStateOf<List<MapContent>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var shouldUpdateClustering by mutableStateOf(false)
        private set

    private var debounceJob: Job? = null
    private var lastRequestLocation: Pair<Double, Double>? = null

    fun loadMapContents(
        latitude: Double,
        longitude: Double,
        radius: Int? = null,
        postType: String? = "ALL",
        date: String? = null
    ) {
        // 이전 요청 취소
        debounceJob?.cancel()
        
        val currentLocation = Pair(latitude, longitude)
        
        // 위치가 크게 변하지 않았으면 요청하지 않음 (500m 이내)
        lastRequestLocation?.let { lastLoc ->
            val distance = calculateDistance(
                lastLoc.first, lastLoc.second,
                latitude, longitude
            )
            if (distance < 500.0) { // 500m 이내면 요청 안함
                return
            }
        }
        
        // 500ms 디바운스 적용
        debounceJob = viewModelScope.launch {
            delay(500)
            
            isLoading = true
            errorMessage = null
            lastRequestLocation = currentLocation

            try {
                val response = mapRepository.getMapContents(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    postType = postType,
                    date = date
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