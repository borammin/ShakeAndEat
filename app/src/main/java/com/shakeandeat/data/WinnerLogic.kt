package com.shakeandeat.data

import kotlin.random.Random

/**
 * The places with the most votes are the candidates, and one of them is chosen at
 * random, so a tie is settled. If nobody has voted yet, every place is a candidate.
 * Shake detection will call the same pick() so the shake and the button give the same result.
 */
object WinnerLogic {
    fun candidates(options: List<PlaceOption>): List<PlaceOption> {
        if (options.isEmpty()) return emptyList()
        val top = options.maxOf { it.votes }
        return if (top == 0) options else options.filter { it.votes == top }
    }

    fun pick(options: List<PlaceOption>, random: Random = Random.Default): PlaceOption? =
        candidates(options).randomOrNull(random)
}

/** Invite codes: six upper-case letters or digits, matching the Firestore rule for group ids. */
object GroupCodes {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // no O, 0, I, 1, L
    private val VALID = Regex("[A-Z0-9]{6}")
    const val LENGTH = 6

    fun normalize(raw: String): String = raw.trim().uppercase()

    fun isValid(code: String): Boolean = VALID.matches(code)

    fun generate(random: Random = Random.Default): String =
        buildString { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
}
