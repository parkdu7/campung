package com.shinhan.campung.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    val userId = mutableStateOf("")
    val password = mutableStateOf("")
    val password2 = mutableStateOf("")
    val nickname = mutableStateOf("")

    val dupChecked = mutableStateOf(false)
    val dupAvailable = mutableStateOf<Boolean?>(null)

    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)

    fun checkDuplicate() {
        val id = userId.value.trim()
        if (id.isBlank()) {
            error.value = "아이디를 입력하세요."
            return
        }
        loading.value = true
        error.value = null
        viewModelScope.launch {
            repo.checkDuplicate(id)
                .onSuccess {
                    dupChecked.value = true
                    dupAvailable.value = it
                    if (!it) error.value = "이미 사용 중인 아이디입니다."
                }
                .onFailure { error.value = it.message ?: "중복 확인 실패" }
            loading.value = false
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        val id = userId.value.trim()
        val pw = password.value
        val pw2 = password2.value
        val nick = nickname.value.trim()

        if (id.isBlank() || pw.isBlank() || pw2.isBlank() || nick.isBlank()) {
            error.value = "모든 항목을 입력하세요."
            return
        }
        if (pw != pw2) {
            error.value = "비밀번호가 일치하지 않습니다."
            return
        }
        if (!(dupChecked.value && dupAvailable.value == true)) {
            error.value = "아이디 중복확인을 완료하세요."
            return
        }

        loading.value = true
        error.value = null
        viewModelScope.launch {
            repo.signUp(id, pw, nick)
                .onSuccess { onSuccess() } // 토큰 저장됨 → 바로 홈으로
                .onFailure { error.value = it.message ?: "회원가입 실패" }
            loading.value = false
        }
    }
}
