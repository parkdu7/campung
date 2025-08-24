package com.shinhan.campung.data.remote.api

import com.shinhan.campung.data.remote.request.DuplicateRequest
import com.shinhan.campung.data.remote.request.LoginRequest
import com.shinhan.campung.data.remote.request.SignUpRequest
import com.shinhan.campung.data.remote.response.DuplicateResponse
import com.shinhan.campung.data.remote.response.LoginResponse
import com.shinhan.campung.data.remote.response.LogoutResponse
import com.shinhan.campung.data.remote.response.SignUpResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("/api/signup")
    suspend fun signUp(@Body body: SignUpRequest): SignUpResponse

    @POST("/api/duplicate")
    suspend fun checkDuplicate(@Body body: DuplicateRequest): DuplicateResponse

    @POST("/api/logout")
    suspend fun logout(): LogoutResponse
}