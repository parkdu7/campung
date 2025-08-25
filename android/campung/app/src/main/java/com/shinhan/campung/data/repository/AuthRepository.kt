package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.remote.api.AuthApi
import com.shinhan.campung.data.remote.request.DuplicateRequest
import com.shinhan.campung.data.remote.request.LoginRequest
import com.shinhan.campung.data.remote.request.SignUpRequest

class AuthRepository(
    private val api: AuthApi,
    private val authDataStore: AuthDataStore
) {
    suspend fun login(userId: String, password: String, fcmToken: String?): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(userId, password, fcmToken))
        authDataStore.saveToken(res.token)
        authDataStore.saveUserId(userId)
    }

    suspend fun signUp(userId: String, password: String, nickname: String): Result<Unit> = runCatching {
        val res = api.signUp(SignUpRequest(userId, password, nickname))
        if (!res.success) error(res.message)
        // ✅ 회원가입 직후에는 로그인시키지 않음 (토큰 저장 X)
        authDataStore.clear() // 혹시 남아있던 토큰도 정리
    }

    suspend fun checkDuplicate(userId: String): Result<Boolean> = runCatching {
        val res = api.checkDuplicate(DuplicateRequest(userId))
        if (!res.success) error(res.message)
        res.available
    }

    // ✅ 서버 로그아웃 + 로컬 토큰 정리
    suspend fun serverLogout(): Result<Unit> = runCatching {
        val res = api.logout()
        if (!res.success) error(res.message)
        authDataStore.clear()
    }

    suspend fun clearLocalToken() { authDataStore.clear() }
}