package com.shinhan.campung.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    protected fun launchWithErrorHandling(
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }

    protected open fun handleError(throwable: Throwable) {
        // 기본 에러 처리 로직
        throwable.printStackTrace()
    }
}