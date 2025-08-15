package com.example.signinwithgoogle.presentation.sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.example.signinwithgoogle.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun sign_in(): IntentSender? {

        val result = try {

            val request = buildSignInRequest()
            oneTapClient.beginSignIn(request).await().also {

            }
        } catch (e: Exception) {

            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    suspend fun getSignInResultFromIntent(intent: Intent): SignInResult {
        Log.d("GoogleAuthUiClient", "Getting SignIn result from intent")
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        Log.d("GoogleAuthUiClient", "Google ID Token: $googleIdToken")

        val googleCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {

            val user = auth.signInWithCredential(googleCredential).await().user

            SignInResult(
                data = user?.run {
                    UserData(
                        userid = uid,
                        username = displayName,
                        profilePictureURL = photoUrl?.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {

            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

   suspend fun signOut() {

        try {
         oneTapClient.signOut().await()
            auth.signOut()

        } catch (e: Exception) {

            if (e is CancellationException) throw e
        }
    }

    fun getSignInUser(): UserData? {
        val currentUser = auth.currentUser

        return currentUser?.run {
            UserData(
                userid = uid,
                username = displayName,
                profilePictureURL = photoUrl.toString()
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {

        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.Web_Client_Id))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
    }
}
