package com.gtr3.byheart.data.remote.api

import com.gtr3.byheart.data.remote.dto.AuthResponseDto
import com.gtr3.byheart.data.remote.dto.GoogleSignInRequestDto
import com.gtr3.byheart.data.remote.dto.LoginRequest
import com.gtr3.byheart.data.remote.dto.ProfileDto
import com.gtr3.byheart.data.remote.dto.RegisterRequest
import com.gtr3.byheart.data.remote.dto.UpdateProfileRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponseDto

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponseDto

    @POST("api/auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequestDto): AuthResponseDto

    @GET("api/auth/profile")
    suspend fun getProfile(): ProfileDto

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): ProfileDto
}
