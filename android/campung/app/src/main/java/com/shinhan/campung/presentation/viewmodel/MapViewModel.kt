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