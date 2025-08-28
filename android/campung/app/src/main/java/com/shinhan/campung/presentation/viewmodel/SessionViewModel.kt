package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shinhan.campung.data.local.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.util.Log
import com.bumptech.glide.Glide.init
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authDataStore: AuthDataStore
) : ViewModel() {

    // ✅ 닉네임을 StateFlow로 노출
    val nickname = authDataStore.nicknameFlow
        .map { it ?: "" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    init {
        authDataStore.nicknameFlow
            .onEach { Log.d("AuthDataStore", "nickname = ${it ?: "null"}") }
            .launchIn(viewModelScope)
    }
}
