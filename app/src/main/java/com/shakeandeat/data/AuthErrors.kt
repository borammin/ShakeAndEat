package com.shakeandeat.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Firebase failure messages */
fun Throwable.toAuthMessage(registering: Boolean): String = when (this) {
    is FirebaseAuthWeakPasswordException ->
        "Choose a stronger password (at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters)."
    is FirebaseAuthUserCollisionException ->
        "An account with this email already exists. Try signing in."
    is FirebaseAuthInvalidCredentialsException ->
        if (registering) "Enter a valid email address." else "Email or password is incorrect."
    is FirebaseAuthInvalidUserException -> "Email or password is incorrect."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
    else -> "Something went wrong. Please try again."
}
