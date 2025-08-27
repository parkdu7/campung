package com.shinhan.campung.data.remote.interceptor

import android.util.Log
import com.shinhan.campung.data.local.AuthDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authDataStore: AuthDataStore
) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 로그인/회원가입/댓글조회 같은 인증 불필요한 엔드포인트는 그대로 진행
        val url = originalRequest.url.toString()
        if (url.contains("/login") || url.contains("/signup") || url.contains("/duplicate") ||
            (url.contains("/comments") && originalRequest.method == "GET")) {
            return chain.proceed(originalRequest)
        }
        
        // 저장된 userId 가져오기
        val userId = runBlocking { authDataStore.userIdFlow.first() }
        
        val requestBuilder = originalRequest.newBuilder()
        
        // Authorization 헤더에 userId 추가
        userId?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
            Log.d(TAG, "Authorization 헤더 추가: $it, URL: $url")
        } ?: run {
            Log.w(TAG, "userId가 null이어서 헤더 추가 안함, URL: $url")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}