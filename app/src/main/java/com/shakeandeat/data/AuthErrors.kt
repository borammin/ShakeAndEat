package com.shakeandeat.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Turns a Firebase failure into a message a person can act on. */
fun Throwable.toAuthMessage(registering: Boolean): String = when (this) {
    // Weak password is a subclass of invalid credentials, so it must come first.
    is FirebaseAuthWeakPasswordException ->
        "Choose a stronger password (at least ${AuthValidation.MIN_PASSWORD_LENGTH} characters)."
    is FirebaseAuthUserCollisionException ->
        "An account with this email already exists. Try signing in."
    is FirebaseAuthInvalidCredentialsException ->
        if (registering) "Enter a valid email address." else "Email or password is incorrect."
    // Deliberately the same wording as a wrong password, so the app does not reveal which emails exist.
    is FirebaseAuthInvalidUserException -> "Email or password is incorrect."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
    else -> "Something went wrong. Please try again."
}
