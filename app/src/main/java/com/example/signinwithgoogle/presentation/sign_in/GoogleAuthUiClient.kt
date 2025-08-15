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
        Log.d("GoogleAuthUiClient", "Starting sign_in()")
        val result = try {
            Log.d("GoogleAuthUiClient", "Building sign-in request...")
            val request = buildSignInRequest()
            oneTapClient.beginSignIn(request).await().also {
                Log.d("GoogleAuthUiClient", "Sign-in request successful")
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthUiClient", "Error in sign_in(): ${e.message}", e)
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
            Log.d("GoogleAuthUiClient", "Signing in with Firebase...")
            val user = auth.signInWithCredential(googleCredential).await().user
            Log.d("GoogleAuthUiClient", "Firebase sign-in successful: ${user?.uid}")
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
            Log.e("GoogleAuthUiClient", "Error in getSignInResultFromIntent(): ${e.message}", e)
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.message
            )
        }
    }

    suspend fun signOut() {
        Log.d("GoogleAuthUiClient", "Signing out...")
        try {
            oneTapClient.signOut().await()
            auth.signOut()
            Log.d("GoogleAuthUiClient", "Sign out successful")
        } catch (e: Exception) {
            Log.e("GoogleAuthUiClient", "Error in signOut(): ${e.message}", e)
            if (e is CancellationException) throw e
        }
    }

    fun getSignInUser(): UserData? {
        val currentUser = auth.currentUser
        Log.d("GoogleAuthUiClient", "Current user: ${currentUser?.uid}")
        return currentUser?.run {
            UserData(
                userid = uid,
                username = displayName,
                profilePictureURL = photoUrl.toString()
            )
        }
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        Log.d("GoogleAuthUiClient", "Building Google Sign-In request object")
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
