package com.example.signinwithgoogle

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.signinwithgoogle.presentation.sign_in.GoogleAuthUiClient
import com.example.signinwithgoogle.presentation.sign_in.ProfileScreen
import com.example.signinwithgoogle.presentation.sign_in.SignInScreen
import com.example.signinwithgoogle.presentation.sign_in.SignInViewModel
import com.example.signinwithgoogle.ui.theme.SignInWithGoogleTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val _googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("DEBUG", "MainActivity onCreate called")
        setContent {
            SignInWithGoogleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "sign_in") {
                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()
                            LaunchedEffect(Unit) {
                                val currentUser = _googleAuthUiClient.getSignInUser()
                                Log.d("DEBUG", "Checking current user: $currentUser")
                                if (currentUser != null) {
                                    Log.d("DEBUG", "Already signed in, navigating to profile")
                                    navController.navigate("profile")
                                }
                            }
                            val launcher =
                                rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                                    onResult = { result ->
                                        Log.d("DEBUG", "Sign-in activity result received: $result")
                                        if (result.resultCode == RESULT_OK) {
                                            lifecycleScope.launch {
                                                val SignInResult =
                                                    _googleAuthUiClient.getSignInResultFromIntent(
                                                        intent = result.data ?: return@launch
                                                    )
                                                Log.d("DEBUG", "Sign-in result: $SignInResult")
                                                viewModel.onSignInResult(SignInResult)
                                            }
                                        }
                                        else {
                                            Log.d("DEBUG", "Sign-in cancelled or failed")
                                        }
                                    }
                                )
                            LaunchedEffect(state.isSignInSuccessful) {
                                Log.d("DEBUG", "Sign-in successful, navigating to profile")
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("profile")
                                    viewModel.resetState()
                                }

                            }
                            SignInScreen(state = state, onSignInClick = {
                                Log.d("DEBUG", "Sign in button clicked")
                                lifecycleScope.launch {
                                    val signInIntentSender = _googleAuthUiClient.sign_in()
                                    if (signInIntentSender != null) {
                                        Log.d("DEBUG", "Launching sign-in intent")
                                        launcher.launch(
                                            IntentSenderRequest.Builder(signInIntentSender).build()
                                        )
                                    } else {
                                        Log.d("DEBUG", "Sign-in intent sender is null")
                                    }
                                }
                            }

                            )
                        }
                        composable("profile") {
                            Log.d("DEBUG", "Profile screen loaded")
                            ProfileScreen(
                                userData = _googleAuthUiClient.getSignInUser(),
                                onSignOut = {
                                    Log.d("DEBUG", "Sign-out clicked")
                                    lifecycleScope.launch {
                                        _googleAuthUiClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.d("DEBUG", "Navigating back after sign-out")
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }}
