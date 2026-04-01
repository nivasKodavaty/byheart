package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.AuthResult

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<AuthResult>
    suspend fun login(email: String, password: String): Result<AuthResult>
    suspend fun saveToken(token: String, refreshToken: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
}
