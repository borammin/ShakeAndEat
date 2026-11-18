package com.shakeandeat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object MessageType {
    const val USER = "user"
    const val PICK = "pick"
}

data class ChatMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val type: String,
    val createdAtMillis: Long?,
)

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUid: String? get() = auth.currentUser?.uid

    private fun messages(gid: String) =
        db.collection("groups").document(gid).collection("messages")

    fun messagesFor(gid: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = messages(gid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(MAX_LOADED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot.documents.map { it.toMessage() })
            }
        awaitClose { registration.remove() }
    }

    suspend fun send(gid: String, text: String) {
        val clean = text.trim()
        require(clean.isNotEmpty())
        require(clean.length <= MAX_LENGTH)
        write(gid, clean, MessageType.USER)
    }

    suspend fun postPick(gid: String, placeName: String) {
        write(gid, "$PICK_PREFIX$placeName".take(MAX_LENGTH), MessageType.PICK)
    }

    private suspend fun write(gid: String, text: String, type: String) {
        val uid = checkNotNull(currentUid)
        messages(gid).add(
            mapOf(
                "senderUid" to uid,
                "senderName" to myName(uid),
                "text" to text,
                "type" to type,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private var cachedName: Pair<String, String>? = null

    private suspend fun myName(uid: String): String {
        cachedName?.let { (cachedUid, name) -> if (cachedUid == uid) return name }
        val fromProfile = runCatching {
            db.collection("users").document(uid).get().await().getString("displayName")
        }.getOrNull()
        val user = auth.currentUser
        val name = fromProfile?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore('@')
            ?: "Someone"
        cachedName = uid to name
        return name
    }

    private fun DocumentSnapshot.toMessage() = ChatMessage(
        id = id,
        senderUid = getString("senderUid").orEmpty(),
        senderName = getString("senderName").orEmpty(),
        text = getString("text").orEmpty(),
        type = getString("type") ?: MessageType.USER,
        createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time,
    )

    companion object {
        const val MAX_LENGTH = 500
        const val PICK_PREFIX = "Today's pick: "
        private const val MAX_LOADED = 200L
    }
}
