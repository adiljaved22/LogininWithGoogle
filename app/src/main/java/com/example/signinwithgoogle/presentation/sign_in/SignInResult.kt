package com.example.signinwithgoogle.presentation.sign_in

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)
data class UserData (
    val userid: String?,
    val username: String?,
    val profilePictureURL: String?

)
