package com.gtr3.byheart.data.remote.interceptor

import com.gtr3.byheart.core.auth.AuthEventBus
import com.gtr3.byheart.data.local.datastore.AuthDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val authEventBus: AuthEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authDataStore.getToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        if (response.code == 401) {
            runBlocking { authDataStore.clearToken() }
            authEventBus.emitUnauthorized()
        }
        return response
    }
}
