package com.shinhan.campung.data.remote.response

import com.shinhan.campung.data.remote.dto.UserDto

//data class LoginResponse(
//    val token: String,
//    val user: UserDto
//)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val accessToken: String,
    val nickname: String
)
