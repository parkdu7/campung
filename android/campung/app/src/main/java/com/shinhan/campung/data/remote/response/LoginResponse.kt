package com.shinhan.campung.data.remote.response

import com.shinhan.campung.data.remote.dto.UserDto

data class LoginResponse(
    val token: String,
    val user: UserDto
)
