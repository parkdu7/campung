package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun logout(onSuccess: () -> Unit) {
        loading.value = true
        error.value = null
        viewModelScope.launch {
            repo.serverLogout()
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "로그아웃 실패" }
            loading.value = false
        }
    }
}
