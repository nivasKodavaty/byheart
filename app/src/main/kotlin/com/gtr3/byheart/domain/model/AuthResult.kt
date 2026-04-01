package com.gtr3.byheart.domain.model

data class AuthResult(
    val token: String,
    val refreshToken: String,
    val email: String
)
