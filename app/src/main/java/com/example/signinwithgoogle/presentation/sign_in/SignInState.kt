package com.example.signinwithgoogle.presentation.sign_in

import okhttp3.Response

data class SignInState(
    val isSignInSuccessful: Boolean=false,
    val signInError: String?=null
)