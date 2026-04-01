package com.gtr3.byheart.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val TOKEN_KEY         = stringPreferencesKey("jwt_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    suspend fun saveToken(token: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[TOKEN_KEY]         = token
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getToken(): String? =
        dataStore.data.map { it[TOKEN_KEY] }.firstOrNull()

    suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[REFRESH_TOKEN_KEY] }.firstOrNull()

    suspend fun clearToken() {
        dataStore.edit { it.clear() }
    }
}
