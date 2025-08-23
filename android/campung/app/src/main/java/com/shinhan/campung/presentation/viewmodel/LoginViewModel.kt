package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    val userId = mutableStateOf("")
    val password = mutableStateOf("")
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun login(fcmToken: String?, onSuccess: () -> Unit) {
        if (userId.value.isBlank() || password.value.isBlank()) {
            error.value = "아이디/비밀번호를 입력하세요."
            return
        }
        loading.value = true
        error.value = null
        viewModelScope.launch {
            repo.login(userId.value, password.value, fcmToken)
                .onSuccess { onSuccess() }
                .onFailure { error.value = it.message ?: "로그인 실패" }
            loading.value = false
        }
    }
}