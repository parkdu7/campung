package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.model.MapContent
import com.shinhan.campung.data.repository.MapContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapContentRepository: MapContentRepository
) : ViewModel() {
    
    // UI States
    private val _bottomSheetContents = MutableStateFlow<List<MapContent>>(emptyList())
    val bottomSheetContents: StateFlow<List<MapContent>> = _bottomSheetContents.asStateFlow()
    
    private val _selectedMarkerId = MutableStateFlow<Long?>(null)
    val selectedMarkerId: StateFlow<Long?> = _selectedMarkerId.asStateFlow()
    
    private val _isBottomSheetExpanded = MutableStateFlow(false)
    val isBottomSheetExpanded: StateFlow<Boolean> = _isBottomSheetExpanded.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 마커 클릭 처리
    fun onMarkerClick(contentId: Long, associatedContentIds: List<Long>) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedMarkerId.value = contentId
            
            mapContentRepository.getContentsByIds(associatedContentIds)
                .onSuccess { contents ->
                    _bottomSheetContents.value = contents
                    _isBottomSheetExpanded.value = true
                }
                .onFailure { 
                    _bottomSheetContents.value = emptyList()
                }
            
            _isLoading.value = false
        }
    }
    
    // 지도 이동시 바텀시트 축소
    fun onMapMove() {
        if (_isBottomSheetExpanded.value) {
            _isBottomSheetExpanded.value = false
        }
    }
    
    // 바텀시트 상태 변경
    fun onBottomSheetStateChange(expanded: Boolean) {
        _isBottomSheetExpanded.value = expanded
    }
    
    // 바텀시트 닫기
    fun clearSelection() {
        _selectedMarkerId.value = null
        _bottomSheetContents.value = emptyList()
        _isBottomSheetExpanded.value = false
    }
}