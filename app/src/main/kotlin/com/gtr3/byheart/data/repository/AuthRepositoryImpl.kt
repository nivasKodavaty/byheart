package com.gtr3.byheart.data.repository

import com.gtr3.byheart.core.util.Result
import com.gtr3.byheart.data.local.datastore.AuthDataStore
import com.gtr3.byheart.data.remote.api.AuthApi
import com.gtr3.byheart.data.remote.dto.LoginRequest
import com.gtr3.byheart.data.remote.dto.RegisterRequest
import com.gtr3.byheart.domain.model.AuthResult
import com.gtr3.byheart.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val dataStore: AuthDataStore
) : AuthRepository {

    override suspend fun register(email: String, password: String): Result<AuthResult> =
        runCatching {
            val response = api.register(RegisterRequest(email, password))
            Result.Success(AuthResult(response.token, response.refreshToken, response.email))
        }.getOrElse { Result.Error(it.message ?: "Registration failed") }

    override suspend fun login(email: String, password: String): Result<AuthResult> =
        runCatching {
            val response = api.login(LoginRequest(email, password))
            Result.Success(AuthResult(response.token, response.refreshToken, response.email))
        }.getOrElse { Result.Error(it.message ?: "Login failed") }

    override suspend fun saveToken(token: String, refreshToken: String) =
        dataStore.saveToken(token, refreshToken)

    override suspend fun getToken(): String? = dataStore.getToken()

    override suspend fun clearToken() = dataStore.clearToken()
}
