package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordUploadViewModel @Inject constructor(
    private val repository: RecordingRepository
) : ViewModel() {
    
    // 업로드 성공 콜백
    private var onUploadSuccessCallback: ((Double, Double) -> Unit)? = null
    
    fun setOnUploadSuccessCallback(callback: (Double, Double) -> Unit) {
        onUploadSuccessCallback = callback
    }

    data class UiState(
        val isUploading: Boolean = false,
        val successMessage: String? = null,
        val errorMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    fun upload(file: File, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _ui.value = UiState(isUploading = true)
            try {
                val res = repository.uploadRecord(file, latitude, longitude)
                if (res.success) {
                    _ui.value = UiState(
                        isUploading = false,
                        successMessage = res.message.ifBlank { "녹음이 등록되었습니다." }
                    )
                    
                    // 업로드 성공 시 콜백 호출 (위치 정보와 함께)
                    onUploadSuccessCallback?.invoke(latitude, longitude)
                } else {
                    _ui.value = UiState(
                        isUploading = false,
                        errorMessage = res.message.ifBlank { "등록 실패" }
                    )
                }
            } catch (e: Exception) {
                _ui.value = UiState(isUploading = false, errorMessage = e.message ?: "에러가 발생했습니다.")
            }
        }
    }

    fun consumeMessages() {
        _ui.value = _ui.value.copy(successMessage = null, errorMessage = null)
    }
}