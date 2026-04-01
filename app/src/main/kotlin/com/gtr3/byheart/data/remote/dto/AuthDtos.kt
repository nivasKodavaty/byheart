package com.gtr3.byheart.data.remote.dto

data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponseDto(val token: String, val refreshToken: String, val email: String)
