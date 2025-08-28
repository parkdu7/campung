package com.shinhan.campung.data.repository

import com.shinhan.campung.data.local.AuthDataStore
import com.shinhan.campung.data.remote.api.AuthApi
import com.shinhan.campung.data.remote.request.DuplicateRequest
import com.shinhan.campung.data.remote.request.LocationShareRespondRequest
import com.shinhan.campung.data.remote.request.LoginRequest
import com.shinhan.campung.data.remote.request.SignUpRequest

class AuthRepository(
    private val api: AuthApi,
    private val authDataStore: AuthDataStore
) {
    suspend fun login(userId: String, password: String, fcmToken: String?): Result<Unit> = runCatching {
        val res = api.login(LoginRequest(userId, password, fcmToken))
        // ✅ 성공 여부 체크 (서버 스펙에 맞춰 필요시 제거/유지)
        if (res.success == false) error(res.message ?: "로그인 실패")

        // ✅ 토큰/유저아이디/닉네임 저장
        authDataStore.saveToken(res.accessToken)   // ← 서버 응답 키 반영
        authDataStore.saveUserId(userId)
        authDataStore.saveNickname(res.nickname)

        // 선택: fcmToken을 로컬에도 보관하고 싶다면
        if (!fcmToken.isNullOrBlank()) {
            authDataStore.saveFcmToken(fcmToken)
        }
    }

    suspend fun signUp(userId: String, password: String, nickname: String): Result<Unit> = runCatching {
        val res = api.signUp(SignUpRequest(userId, password, nickname))
        if (!res.success) error(res.message)
        // 회원가입 직후 자동 로그인하지 않으므로 로컬 정리
        authDataStore.clear()
    }

    suspend fun checkDuplicate(userId: String): Result<Boolean> = runCatching {
        val res = api.checkDuplicate(DuplicateRequest(userId))
        if (!res.success) error(res.message)
        res.available
    }

    suspend fun serverLogout(): Result<Unit> = runCatching {
        val res = api.logout()
        if (!res.success) error(res.message)
        authDataStore.clear()
    }

    suspend fun clearLocalToken() { authDataStore.clear() }

    suspend fun updateFcmToken(fcmToken: String): Result<Unit> = runCatching {
        // TODO: 서버에 FCM 토큰 업데이트 API 붙이면 여기서 호출
        authDataStore.saveFcmToken(fcmToken)
    }

    suspend fun respondToLocationShareRequest(
        shareRequestId: Long,
        action: String,
        latitude: java.math.BigDecimal?,
        longitude: java.math.BigDecimal?
    ): Result<Unit> = runCatching {
        val request = LocationShareRespondRequest(action, latitude, longitude)
        val res = api.respondToLocationShare(shareRequestId, request)
        if (!res.success) error(res.message)
    }
}
