package com.gtr3.byheart.data.remote.api

import com.gtr3.byheart.data.remote.dto.AuthResponseDto
import com.gtr3.byheart.data.remote.dto.LoginRequest
import com.gtr3.byheart.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponseDto
}
