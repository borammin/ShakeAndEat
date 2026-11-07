package com.shakeandeat.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Email/password sign-in with Firebase Authentication. Every account also gets a profile
 * document at users/{uid}, which other group members read to show names.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Emits the signed-in user, or null, now and on every change. */
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        val user = auth.signInWithEmailAndPassword(email, password).await().user ?: return
        ensureProfile(user)
    }

    suspend fun register(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user ?: return
        bestEffort("set display name") {
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        }
        bestEffort("create profile") { createProfile(user.uid, name, email) }
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    /** Accounts made before the profile existed, or a failed first write, get one on next sign-in. */
    private suspend fun ensureProfile(user: FirebaseUser) = bestEffort("ensure profile") {
        val ref = db.collection(USERS).document(user.uid)
        if (!ref.get().await().exists()) {
            val email = user.email.orEmpty()
            createProfile(user.uid, user.displayName ?: email.substringBefore('@'), email)
        }
    }

    private suspend fun createProfile(uid: String, name: String, email: String) {
        db.collection(USERS).document(uid).set(
            mapOf(
                "displayName" to name,
                "email" to email,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Signing in must not fail just because the profile write did; log it and carry on. */
    private suspend fun bestEffort(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not $what", e)
        }
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val USERS = "users"
    }
}
