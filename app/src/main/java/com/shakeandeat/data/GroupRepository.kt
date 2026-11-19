package com.shakeandeat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Everything the app reads and writes in Cloud Firestore for lunch groups.
 */
class GroupRepository(
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUid: String? = auth.currentUser?.uid

    private val groups get() = db.collection("groups")

    /** List of the groups this user belongs to. */
    fun myGroups(uid: String): Flow<List<Group>> = callbackFlow {
        val registration = groups.whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(
                        snapshot.documents.mapNotNull { it.toGroup() }
                            .sortedBy { it.name.lowercase() },
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    /** View of one group. Emits null if it does not exist. */
    fun group(gid: String): Flow<Group?> = callbackFlow {
        val registration = groups.document(gid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(snapshot.toGroup())
        }
        awaitClose { registration.remove() }
    }

    /** List of the places in a group, oldest first. */
    fun options(gid: String): Flow<List<PlaceOption>> = callbackFlow {
        val registration = groups.document(gid).collection("options")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(
                        snapshot.documents.mapNotNull { it.toPlaceOption() }
                            .sortedWith(compareBy({ it.createdAtMillis }, { it.name.lowercase() })),
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Creates a group owned by [uid] and returns its invite code. The code is the document id.
     * Writing over an existing group is refused by the rules, so a code that is already taken
     * simply fails and a new one is tried.
     */
    suspend fun createGroup(name: String, uid: String): String {
        var lastError: Exception? = null
        repeat(MAX_CODE_ATTEMPTS) {
            val code = GroupCodes.generate()
            try {
                groups.document(code).set(
                    mapOf(
                        "name" to name,
                        "ownerUid" to uid,
                        "memberUids" to listOf(uid),
                        "status" to GroupStatus.VOTING,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                return code
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                if (!e.isPermissionDenied()) throw e
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Could not create a group")
    }

    /**
     * Joins by invite code.
     */
    suspend fun joinGroup(code: String, uid: String) {
        groups.document(code).update("memberUids", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun addOption(
        gid: String,
        name: String,
        uid: String,
        placeId: String? = null,
        distanceMi: Double? = null,
    ) {
        val data = mutableMapOf<String, Any>(
            "name" to name,
            "addedByUid" to uid,
            "voteUids" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        placeId?.let { data["placeId"] = it }
        distanceMi?.let { data["distanceMi"] = it }
        groups.document(gid).collection("options").document().set(data).await()
    }

    /** Deletes a place. The rules allow only the member who added it, or the group owner. */
    suspend fun removeOption(gid: String, optionId: String) {
        groups.document(gid).collection("options").document(optionId).delete().await()
    }

    /** Adds or removes only the caller's own uid from the option's votes. */
    suspend fun setVote(gid: String, optionId: String, uid: String, voted: Boolean) {
        val change = if (voted) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        groups.document(gid).collection("options").document(optionId).update("voteUids", change).await()
    }

    suspend fun decide(gid: String, winnerOptionId: String) {
        groups.document(gid).update(
            mapOf(
                "status" to GroupStatus.DECIDED,
                "winnerOptionId" to winnerOptionId,
                "decidedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun reopenVoting(gid: String) {
        groups.document(gid).update(
            mapOf(
                "status" to GroupStatus.VOTING,
                "winnerOptionId" to FieldValue.delete(),
                "decidedAt" to FieldValue.delete(),
            ),
        ).await()
    }

    private companion object {
        const val MAX_CODE_ATTEMPTS = 5
    }
}
