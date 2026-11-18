package com.shakeandeat.data

import com.google.firebase.firestore.DocumentSnapshot

object GroupStatus {
    const val VOTING = "voting"
    const val DECIDED = "decided"
}

/** groups/{code}. The document id is the six-character invite code. */
data class Group(
    val id: String,
    val name: String,
    val ownerUid: String,
    val memberUids: List<String>,
    val status: String,
    val winnerOptionId: String?,
    val decidedAtMillis: Long? = null,
) {
    val isDecided: Boolean get() = status == GroupStatus.DECIDED
}

/** groups/{code}/options/{id}: one place the group can vote on. */
data class PlaceOption(
    val id: String,
    val name: String,
    val addedByUid: String,
    val voteUids: List<String>,
    val createdAtMillis: Long,
    val placeId: String? = null,
    val distanceMi: Double? = null,
) {
    val votes: Int get() = voteUids.size
}

private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

fun DocumentSnapshot.toGroup(): Group? {
    if (!exists()) return null
    return Group(
        id = id,
        name = getString("name") ?: return null,
        ownerUid = getString("ownerUid").orEmpty(),
        memberUids = stringList("memberUids"),
        status = getString("status") ?: GroupStatus.VOTING,
        winnerOptionId = getString("winnerOptionId"),
        decidedAtMillis = getTimestamp("decidedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time,
    )
}

fun DocumentSnapshot.toPlaceOption(): PlaceOption? {
    if (!exists()) return null
    return PlaceOption(
        id = id,
        name = getString("name") ?: return null,
        addedByUid = getString("addedByUid").orEmpty(),
        voteUids = stringList("voteUids"),
        createdAtMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: 0L,
        placeId = getString("placeId"),
        distanceMi = getDouble("distanceMi"),
    )
}
