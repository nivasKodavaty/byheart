package com.gtr3.byheart.data.remote.dto

data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponseDto(val token: String, val refreshToken: String, val email: String)

data class ProfileDto(
    val email: String,
    val displayName: String?,
    val dateOfBirth: String?,
    val sex: String?
)

data class UpdateProfileRequestDto(
    val displayName: String?,
    val dateOfBirth: String?,
    val sex: String?
)

data class GoogleSignInRequestDto(val idToken: String)
