package com.shakeandeat.data

import com.google.firebase.firestore.FirebaseFirestoreException

/** Firestore failure messages */
fun Throwable.toGroupMessage(): String {
    val code = (this as? FirebaseFirestoreException)?.code
    return when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "No internet connection. Check your network and try again."
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have access to that."
        else -> "Something went wrong. Please try again."
    }
}

fun Throwable.isPermissionDenied(): Boolean =
    (this as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
