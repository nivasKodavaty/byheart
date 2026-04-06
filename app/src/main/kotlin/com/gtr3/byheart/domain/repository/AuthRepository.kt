package com.gtr3.byheart.domain.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.domain.model.AuthResult
import com.gtr3.byheart.domain.model.UserProfile

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<AuthResult>
    suspend fun login(email: String, password: String): Result<AuthResult>
    suspend fun saveToken(token: String, refreshToken: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
    suspend fun loginWithGoogle(idToken: String): Result<AuthResult>
    suspend fun getProfile(): Result<UserProfile>
    suspend fun updateProfile(displayName: String?, dateOfBirth: String?, sex: String?): Result<UserProfile>
}
