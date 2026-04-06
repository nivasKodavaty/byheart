package com.gtr3.byheart.core.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.gtr3.byheart.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInHelper @Inject constructor() {

    /**
     * Launches the Google Sign-In bottom sheet and returns the ID token on success.
     * Must be called with an Activity context (not application context) so the
     * system can anchor the bottom sheet to the current window.
     *
     * Throws:
     *  - [GetCredentialCancellationException] if the user dismissed the picker
     *  - [Exception] for any other failure
     */
    suspend fun signIn(activity: Activity): String {
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = activity
        )

        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }

        throw Exception("Unexpected credential type: ${credential.type}")
    }
}
